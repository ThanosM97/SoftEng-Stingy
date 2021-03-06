package gr.ntua.ece.stingy.data;
/*
 * A class that converts the result of a query in the product table into a product object. 
 */

import gr.ntua.ece.stingy.conf.Configuration;
import gr.ntua.ece.stingy.data.model.Product;

import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

public class ProductRowMapper implements RowMapper {
	private final DataAccess dataAccess = Configuration.getInstance().getDataAccess();

	@Override
	public Product mapRow(ResultSet rs, int rowNum) throws SQLException {

		long id            = rs.getLong("id");
		String name        = rs.getString("name");
		String description = rs.getString("description");
		String category    = rs.getString("category");
		boolean withdrawn  = rs.getBoolean("withdrawn");
		String image	   = rs.getString("image");
		/*
		 * Get tags and extraData of the product.
		 */
		List<String> tags = dataAccess.getProductTagsById(String.valueOf(id));
		Map<String, String> extraData = dataAccess.getExtraDataById(id); 
		Double bestPrice = dataAccess.getBestPrice(id);
		return new Product(id, name, description, category, withdrawn, tags, extraData, bestPrice, image);
	}

}
