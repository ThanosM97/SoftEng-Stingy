package gr.ntua.ece.stingy.data;
/*
 * A class implementing all the queries in the database.
 */

import gr.ntua.ece.stingy.data.model.Product;
import gr.ntua.ece.stingy.data.model.Message;
import gr.ntua.ece.stingy.data.model.Record;
import gr.ntua.ece.stingy.data.model.Shop;
import org.apache.commons.dbcp2.BasicDataSource;
import org.restlet.data.Status;
import org.restlet.resource.ResourceException;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.core.RowCountCallbackHandler;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class DataAccess {

	private static final Object[] EMPTY_ARGS = new Object[0];
	private static final int MAX_TOTAL_CONNECTIONS = 16;
	private static final int MAX_IDLE_CONNECTIONS = 8;
	private DataSource dataSource;
	private JdbcTemplate jdbcTemplate;
	private NamedParameterJdbcTemplate namedJdbcTemplate;

	public void setup(String driverClass, String url, String user, String pass) throws SQLException {

		// Initialize the data source
		BasicDataSource bds = new BasicDataSource();
		bds.setDriverClassName(driverClass);
		bds.setUrl(url);
		bds.setMaxTotal(MAX_TOTAL_CONNECTIONS);
		bds.setMaxIdle(MAX_IDLE_CONNECTIONS);
		bds.setUsername(user);
		bds.setPassword(pass);
		bds.setValidationQuery("SELECT 1");
		bds.setTestOnBorrow(true);
		bds.setDefaultAutoCommit(true);

		// Check that everything works OK
		bds.getConnection().close();

		// Initialize the jdbc template utility
		jdbcTemplate = new JdbcTemplate(bds);
		namedJdbcTemplate = new NamedParameterJdbcTemplate(bds);
	}

	public List<Product> getProducts(Limits limits, String status, String sort) {
		String sort_type = sort.replaceAll("\\|", " ");   
		/*
		 * Initialize withdrawn based on the status value
		 */
		String withdrawn = null;
		if (status.equals("ALL")) {
			withdrawn = "withdrawn";
		}
		else if (status.equals("WITHDRAWN")) {
			withdrawn = "1";
		}
		else {
			withdrawn = "0";
		}

		/*
		 * Get number of all products
		 */
		RowCountCallbackHandler countCallback = new RowCountCallbackHandler();  // not reusable
		jdbcTemplate.query("select * from Product order by id", countCallback);
		int rowCount = countCallback.getRowCount();
		limits.setTotal(rowCount);
		/*
		 * Return products based on the limits.
		 */
		//TODO: fix error in descending order 
		return jdbcTemplate.query("select * from Product where withdrawn=? order by ? limit ?,?", new Object[] { withdrawn, sort_type, limits.getStart(), limits.getCount() }, new ProductRowMapper());
	}
	
	public List<String> getTagsById(long id){
		String query = "select distinct Tag.name from Product_Tag, Tag where productId=? and tagId = Tag.id";
		return jdbcTemplate.queryForList(query, new Object[] { id }, String.class);
	}
	
	public Map<String, String> getExtraDataById(long id){
		String query = "SELECT extraData.characteristic, extraData.value FROM extraData where productId = ?";
		return jdbcTemplate.query(query ,new Object[] { id }, new ResultSetExtractor<Map>(){
		    @Override
		    public Map extractData(ResultSet rs) throws SQLException,DataAccessException {
		        HashMap<String,String> mapRet= new HashMap<String,String>();
		        while(rs.next()){
		            mapRet.put(rs.getString("characteristic"),rs.getString("value"));
		        }
		        return mapRet;
		    }
		});
	}

	public Product addProduct(String name, String description, String category, boolean withdrawn, ArrayList<String> tags, String extraDataString ) {
		/*
		 * Insert the new product in the Product table
		 */
		KeyHolder keyHolder = new GeneratedKeyHolder();	// for keeping the product id
		jdbcTemplate.update(
		    new PreparedStatementCreator() {
		        public PreparedStatement createPreparedStatement(Connection connection) throws SQLException {
		            PreparedStatement ps =
		                connection.prepareStatement("insert into Product(name, description, category, withdrawn) "
		                		+ "values(?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS);
		            ps.setString(1, name);
					ps.setString(2, description);
					ps.setString(3, category);
					ps.setBoolean(4, withdrawn);
		            return ps;
		        }
		    },
		    keyHolder);
		System.out.println(keyHolder.getKey());
		long productId = (long)keyHolder.getKey();
		/*
		 * For each tag insert it in the Tag table if not exists and then insert it in the Product_Tag table.
		 */
		Long tagId;
		int count;
		for (String tag: tags) { 
			count = jdbcTemplate.queryForObject("select count(*) from Tag where "
					+ "name=?", new Object[] { tag }, Integer.class);
			if (count > 0) {
				tagId = jdbcTemplate.queryForObject("select id from Tag where "
						+ "name=?", new Object[] { tag }, Long.class);
			}
			else {
				/*
				 * Insert tag in Tag table.
				 */
				keyHolder = new GeneratedKeyHolder();
				jdbcTemplate.update(
				    new PreparedStatementCreator() {
				        public PreparedStatement createPreparedStatement(Connection connection) throws SQLException {
				            PreparedStatement ps =
				                connection.prepareStatement("INSERT INTO Tag(name) VALUES(?)", Statement.RETURN_GENERATED_KEYS);
				            ps.setString(1, tag);
				            return ps;
				        }
				    },
				    keyHolder);
				tagId = (long)keyHolder.getKey();
			}
			jdbcTemplate.update("INSERT INTO Product_Tag(ProductId, TagId) VALUES(?, ?)", new Object[] { productId, tagId  });			
		}
		List<String> extraDataList = Arrays.asList(extraDataString.split(","));
		Map<String, String> extraData = new HashMap<>();
		if (category.equals("Laptop")) {
			if (extraDataList.size() != 6) {
				throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST, "Invalid extra data size: " + extraDataList.size() + " instead of 6");
			}
			/*
			 * Extra data supported for Laptops
			 */
			extraData.put("CPU", extraDataList.get(0));
			jdbcTemplate.update("INSERT INTO extraData(characteristic, value, ProductId ) VALUES(CPU, ?, ?)", new Object[] { extraDataList.get(0), productId});			
			extraData.put("RAM", extraDataList.get(1));
			jdbcTemplate.update("INSERT INTO extraData(characteristic, value, ProductId ) VALUES(RAM, ?, ?)", new Object[] { extraDataList.get(1), productId});			
			extraData.put("Hard Drive", extraDataList.get(2));
			jdbcTemplate.update("INSERT INTO extraData(characteristic, value, ProductId ) VALUES(Hard Drive, ?, ?)", new Object[] { extraDataList.get(2), productId});			
			extraData.put("OS", extraDataList.get(3));
			jdbcTemplate.update("INSERT INTO extraData(characteristic, value, ProductId ) VALUES(OS, ?, ?)", new Object[] { extraDataList.get(3), productId});			
			extraData.put("Screen Size", extraDataList.get(4));
			jdbcTemplate.update("INSERT INTO extraData(characteristic, value, ProductId ) VALUES(Screen Size, ?, ?)", new Object[] {  extraDataList.get(4), productId});			
			extraData.put("Graphics Card", extraDataList.get(5));
			jdbcTemplate.update("INSERT INTO extraData(characteristic, value, ProductId ) VALUES(Graphics Card, ?, ?)", new Object[] { extraDataList.get(5), productId});			

		}
		else if (category.equals("TV")) {
			if (extraDataList.size() != 3) {
				throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST, "Invalid extra data size: " + extraDataList.size() + " instead of 3");
			}
			/*
			 * Extra data supported for TVs
			 */
			if (extraDataList.get(0).equals("4k") || extraDataList.get(0).equals("4K")) {
				extraData.put("4K", "Yes");
			}
			else {
				extraData.put("4K", "No");
			}
			if (extraDataList.get(1).equals("Smart") || extraDataList.get(1).equals("SMART")) {
				extraData.put("Smart", "Yes");
			}
			else {
				extraData.put("Smart", "No");
			}			
			extraData.put("Frequency", extraDataList.get(2));
			jdbcTemplate.update("INSERT INTO extraData(characteristic, value, ProductId ) VALUES(4K, ?, ?)", new Object[] { extraDataList.get(0), productId});			
			jdbcTemplate.update("INSERT INTO extraData(characteristic, value, ProductId ) VALUES(Smart, ?, ?)", new Object[] { extraDataList.get(1), productId});			
			jdbcTemplate.update("INSERT INTO extraData(characteristic, value, ProductId ) VALUES(Frequency, ?, ?)", new Object[] { extraDataList.get(2), productId});			

		}
		else if (category.equals("Smartphone")) {
			if (extraDataList.size() != 7) {
				throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST, "Invalid extra data size: " + extraDataList.size() + " instead of 7");
			}
			/*
			 * Extra data supported for Smartphones
			 */
			extraData.put("CPU cores", extraDataList.get(0));
			jdbcTemplate.update("INSERT INTO extraData(characteristic, value, ProductId ) VALUES(CPU cores, ?, ?)", new Object[] { extraDataList.get(0), productId});			
			extraData.put("CPU frequency", extraDataList.get(1));
			jdbcTemplate.update("INSERT INTO extraData(characteristic, value, ProductId ) VALUES(CPU frequency, ?, ?)", new Object[] { extraDataList.get(1), productId});			
			extraData.put("RAM", extraDataList.get(2));
			jdbcTemplate.update("INSERT INTO extraData(characteristic, value, ProductId ) VALUES(RAM, ?, ?)", new Object[] { extraDataList.get(2), productId});			
			extraData.put("Capacity", extraDataList.get(3));
			jdbcTemplate.update("INSERT INTO extraData(characteristic, value, ProductId ) VALUES(Capacity, ?, ?)", new Object[] { extraDataList.get(3), productId});			
			extraData.put("Front camera", extraDataList.get(4));
			jdbcTemplate.update("INSERT INTO extraData(characteristic, value, ProductId ) VALUES(Front camera, ?, ?)", new Object[] { extraDataList.get(4), productId});			
			extraData.put("Selfie camera", extraDataList.get(5));
			jdbcTemplate.update("INSERT INTO extraData(characteristic, value, ProductId ) VALUES(Selfie camera, ?, ?)", new Object[] { extraDataList.get(5), productId});			
			extraData.put("OS", extraDataList.get(6));
			jdbcTemplate.update("INSERT INTO extraData(characteristic, value, ProductId ) VALUES(OS, ?, ?)", new Object[] { extraDataList.get(6), productId});			
		}
		else {
			throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST, "Category " + category + " is not supported in stingy");
		}
		/*
		* Create product and return it.
		*/
		Product product = new Product(
					productId, //the newly created project id
					name,
					description,
					category,
					withdrawn,
					tags,
					extraData
					);
		return product;
}

	public Optional<Product> getProduct(long id) {
		Long[] params = new Long[]{id};
		List<Product> products = jdbcTemplate.query("select * from product where id = ?", params, new ProductRowMapper());
		if (products.size() == 1)  {
			return Optional.of(products.get(0));
		}
		else {
			return Optional.empty();
		}
	}

	public Optional<Message> deleteProduct(long id) {
		Long[] params = new Long[]{id};
		int found = jdbcTemplate.update("delete from product where id=?", params);
		if (found == 1)  {
			return Optional.of(new Message("OK"));
		}
		else {
			return Optional.empty();
		}
	}

	public Optional<Product> updateProduct(long id, String name, String description, String category, boolean withdrawn, String tagsString, String extraDataString ) {
		// Updates the new product record
		int rows = jdbcTemplate.update("update product set name=?, description=?, category=?, withdrawn=?, tags =?, extraData=? where id =?", new Object[] {name, description, category, withdrawn, tagsString, extraDataString, id});
		System.out.println(rows);
		// Check if the product exists
		if (rows == 1)  {
			// return the product that was updated.
			return getProduct(id);
		}
		else {
			return Optional.empty();
		}
	}

	public Optional<Product> patchProduct(long id, String value, String field ) {
		// Updates the new product record based on the non null value
		int rows;
		if (field.equals("withdrawn")) {
			boolean withdrawn = Boolean.valueOf(value);
			rows = jdbcTemplate.update("update product set " + field + "=? where id =?", new Object[] {withdrawn, id});
		}
		else {
			rows = jdbcTemplate.update("update product set " + field + "=? where id =?", new Object[] {value, id});
		}
		// Check if the product exists
		if (rows == 1)  {
			// return the product that was updated.
			return getProduct(id);
		}
		else {
			return Optional.empty();
		}
	}

	public List<Shop> getShops(Limits limits, String status, String sort) {
		String sort_type = sort.replaceAll("\\|", " ");   
		/*
		 * initialize withdrawn based on the status value
		 */
		String withdrawn = null;
		if (status.equals("ALL")) {
			withdrawn = "withdrawn";
		}
		else if (status.equals("WITHDRAWN")) {
			withdrawn = "1";
		}
		else {
			withdrawn = "0";
		}

		/*
		 * get number of shops
		 */
		RowCountCallbackHandler countCallback = new RowCountCallbackHandler();  // not reusable
		jdbcTemplate.query("select * from shop order by id", countCallback);
		int rowCount = countCallback.getRowCount();
		limits.setTotal(rowCount);
		/*
		 * return shops based on the limits.
		 */
		//TODO: fix error in descending order 
		return jdbcTemplate.query("select * from shop where withdrawn=? order by ? limit ?,?", new Object[] { withdrawn,sort_type, limits.getStart(), limits.getCount() }, new ShopRowMapper());
	}

	public Shop addShop(String name, String address,double lng, double lat, String tags, boolean withdrawn ) {
		//Create the new shop record using a prepared statement
		PreparedStatementCreator psc = new PreparedStatementCreator() {
			@Override
			public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
				PreparedStatement ps = con.prepareStatement(
						"insert into shop(name, address, lng, lat, tags, withdrawn) values(?, ?, ?, ?, ?,?)",
						Statement.RETURN_GENERATED_KEYS
						);
				ps.setString(1, name);
				ps.setString(2, address);
				ps.setDouble(3,  lng);
				ps.setDouble(4, lat);
				ps.setString(5, tags);
				ps.setBoolean(6, withdrawn);
				return ps;
			}
		};
		GeneratedKeyHolder keyHolder = new GeneratedKeyHolder();
		int cnt = jdbcTemplate.update(psc, keyHolder);

		if (cnt == 1) {
			//New row has been added
			Shop shop = new Shop(
					keyHolder.getKey().longValue(), //the newly created project id
					name,
					address,
					lng,
					lat,
					tags,
					withdrawn
					);
			return shop;

		}
		else {
			throw new RuntimeException("Creation of Product failed");
		}
	}

	public Optional<Shop> getShop(long id) {
		Long[] params = new Long[]{id};
		List<Shop> shops = jdbcTemplate.query("select * from shop where id = ?", params, new ShopRowMapper());
		if (shops.size() == 1)  {
			return Optional.of(shops.get(0));
		}
		else {
			return Optional.empty();
		}
	}

	public Optional<Message> deleteShop(long id) {
		Long[] params = new Long[]{id};
		int found = jdbcTemplate.update("delete from shop where id=?", params);
		if (found == 1)  {
			return Optional.of(new Message("OK"));
		}
		else {
			return Optional.empty();
		}
	}

	public Optional<Shop> updateShop(long id, String name, String address, double lng, double lat, String tags, boolean withdrawn ) {
		// Updates the new shop record
		int rows = jdbcTemplate.update("update shop set name=?, address=?, lng=?, lat=?, tags =?, withdrawn=? where id =?", new Object[] {name, address, lng, lat, tags, withdrawn, id});
		System.out.println(rows);
		// Check if the product exists
		if (rows == 1)  {
			// return the product that was updated.
			return getShop(id);
		}
		else {
			return Optional.empty();
		}
	}

	public Optional<Shop> patchShop(long id, String value, String field ) {
		// Updates the new shop record based on the non null value
		int rows;
		if (field.equals("withdrawn")) {
			boolean withdrawn = Boolean.valueOf(value);
			rows = jdbcTemplate.update("update shop set " + field + "=? where id =?", new Object[] {withdrawn, id});
		}
		else if (field.equals("lng")) {
			double lng = Double.valueOf(value);
			rows = jdbcTemplate.update("update shop set " + field + "=? where id =?", new Object[] {lng, id});
		}
		else if (field.equals("lat")) {
			double lat = Double.valueOf(value);
			rows = jdbcTemplate.update("update shop set " + field + "=? where id =?", new Object[] {lat, id});
		}
		else {
			rows = jdbcTemplate.update("update shop set " + field + "=? where id =?", new Object[] {value, id});
		}
		// Check if the shop exists
		if (rows == 1)  {
			// return the product that was updated.
			return getShop(id);
		}
		else {
			return Optional.empty();
		}
	}
	
	
	public List<Record> getRecords(Limits limits, String geoDistString, String geoLngString, String geoLatString, String dateFrom, String dateTo, 
			String shops, String products, String tags , String sort) {
		String sort_type = sort.replaceAll("\\|", " ");
		/*
		 * Initialize withdrawn based on the status value
		 */
		/*
		 * Get number of all products
		 */
		
		/*
		 * Return products based on the limits.
		 */
		
		//shopTagsNew
		MapSqlParameterSource parameters = new MapSqlParameterSource();
		parameters.addValue("geoLng", geoLngString);
	    parameters.addValue("geoLat", geoLatString);
	    parameters.addValue("geoDist", geoDistString);
	    parameters.addValue("dateFrom", dateFrom);
	    parameters.addValue("dateTo", dateTo);
	    parameters.addValue("shops", shops);
	    parameters.addValue("products", products);
	    parameters.addValue("productTags", tags);
	    parameters.addValue("shopTags", tags);
	    parameters.addValue("sort", sort_type);
	    parameters.addValue("start", limits.getStart());
	    parameters.addValue("count", limits.getCount());

	    System.out.println(geoLngString);
	    System.out.println(geoLatString);
	    System.out.println(geoDistString);
	    System.out.println(dateFrom);
	    System.out.println(dateTo);
	    System.out.println(shops);
	    System.out.println(products);
	    System.out.println(tags);
	    System.out.println(sort_type);
	    String sqlStm;
	    
	    if (geoDistString != null) {
	    	sqlStm = "SELECT price, product.id as productId, product.name as productName, product.tags as productTags, shop.id as shopId, shop.name as shopName, shop.tags as shopTags, shop.address, \n" + 
					"SQRT(POW(shop.lng - :geoLng, 2) + POW(shop.lat - :geoLat, 2)) as dist, record.date\n" + 
					"FROM shop, product, record\n" + 
					"WHERE SQRT(POW(shop.lng - :geoLng, 2) + POW(shop.lat - :geoLat, 2)) < :geoDist\n" + 
					"AND record.shopId = shop.id \n" + 
					"AND record.productId = product.id\n" + 
					"AND record.date > :dateFrom and record.date <= :dateTo\n";
	    
	    }
	    else {
	    	sqlStm = "SELECT price, product.id as productId, product.name as productName, product.tags as productTags, shop.id as shopId, shop.name as shopName, shop.tags as shopTags, shop.address, -1 as dist, \n" + 
					" record.date\n" + 
					"FROM shop, product, record\n" + 
					"WHERE \n" + 
					"record.shopId = shop.id \n" + 
					"AND record.productId = product.id\n" + 
					"AND record.date > :dateFrom and record.date <= :dateTo\n";
	    }
	    
	    if (products != null) {
	    	sqlStm += "AND record.productId in (:products)\n";
	    }
	    if (shops != null) {
	    	sqlStm += "AND record.shopId in (:shops)\n";
	    }
	    if (tags != null) {
	    	sqlStm += "AND (\n" + 
					"product.tags REGEXP :productTags\n" + 
					"OR shop.tags REGEXP :shopTags\n" + 
					")\n";
	    }
	    
	    RowCountCallbackHandler countCallback = new RowCountCallbackHandler();  // not reusable
	    namedJdbcTemplate.query(sqlStm, parameters, countCallback);
		int rowCount = countCallback.getRowCount();
		limits.setTotal(rowCount);
	    
	    sqlStm += "order by :sort limit :start, :count";
	    System.out.println(sqlStm);
	    return namedJdbcTemplate.query(sqlStm, parameters, new RecordRowMapper());
	}
	
}
