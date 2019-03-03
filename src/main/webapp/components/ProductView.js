import React from 'react';
import ReactDOM from 'react-dom';
import Map from './Map';
//import { NONAME } from 'dns';


const smartphone = "../static/images/smartphone250x250.png";
const like = "../static/images/like.png";
const unlike = "../static/images/unlike.png";
const questionmark = "../static/images/questionmark.png";


export class ProductView extends React.Component{
    render(){
        // We should give as the product argument the 'category' argument from the URL
        // For testing I am going to assume the URL is of type www.stingy.com/category='category'&
        return(
            <div>
                <ProductViewSpecs class='main-product' product={this.props.product}/>  
                <ProductViewShopList class='main-shops' shops={this.props.shops}/>
            </div>
        );
    }
}

export class ProductViewSpecs extends React.Component{
    render(){
        let product_render;
        switch (this.props.product.device){
            case 'smartphone':
                product_render = ( <ProductViewSmartphone product={this.props.product} />);
                break;
            case 'tv':
                product_render = ( <ProductViewTv product={this.props.product} />);
                break;
            case 'laptop':
                product_render = ( <ProductViewLaptop product={this.props.product} />);
                break;
            default:
                product_render = ( <div> Oops! Invalid product type :( </div> );
            }

        return(
                <div class='specs'>
                    <div>{product_render}</div>
                </div>
        );
    }
}

class ProductViewSmartphone extends React.Component{
    render(){
        return(
            <div>
                <a class='product-name'>{this.props.product.name}</a><br />
                <img src={smartphone} class='product-image' /><br />
                <table class='specs-table'>
                    <tr>
                        <td>Λειτουργικό Σύστημα:</td> 
                        <td>{this.props.product.OS}</td>
                    </tr>
                    <tr>
                        <td>Οθόνη:</td> 
                        <td>{this.props.product.size}</td>
                    </tr>
                    <tr>
                        <td>Μνήμη RAM:</td> 
                        <td>{this.props.product.ram}</td>
                    </tr>
                    <tr>
                        <td>Χωρητικότητα:</td> 
                        <td>{this.props.product.capacity}</td>
                    </tr>
                    <tr>
                        <td>Κάμερα:</td> 
                        <td>{this.props.product.backCamera}</td>
                    </tr>
                    <tr>
                        <td>Selfie:</td> 
                        <td>{this.props.product.frontCamera}</td>
                    </tr>
                    <tr>
                        <td>Πυρήνες Επεξεργαστή:</td> 
                        <td>{this.props.product.cpuCores}</td>
                    </tr>
                    <tr>
                        <td>Ταχύτητα Επεξεργαστή:</td> 
                        <td>{this.props.product.cpuFrequency}</td>
                    </tr>
                </table>
            </div>
        );
    }
}

class ProductViewTv extends React.Component{
    render(){
        return(
            <div>
                <a class='product-name'>{this.props.product.name}</a><br />
                <img src={smartphone} class='product-image' /><br />
                <table class='specs-table'></table>
                    <tr>
                        <td>4K:</td>
                        <td>{this.props.product.fourk}</td>
                    </tr>
                    <tr>
                        <td>Smart:</td>
                        <td>{this.props.product.smart}</td>
                    </tr>
                    <tr>
                        <td>Συχνότητα:</td>
                        <td>{this.props.product.frequency}</td>
                    </tr>
            </div>
        );
    }
}

class ProductViewLaptop extends React.Component{
	render(){
		return(
            <div>
                <a class='product-name'>{this.props.product.name}</a><br />
                <img src={laptop} class='product-image' /><br />
                <table class='specs-table'></table>
                    <tr>
                        <td>CPU:</td>
                        <td>{this.props.product.cpu}</td>
                    </tr>
                    <tr>
                        <td>RAM:</td>
                        <td>{this.props.product.ram}</td>
                    </tr>
                    <tr>
                        <td>Σκληρός Δίσκος:</td>
                        <td>{this.props.product.harddrive}</td>
                    </tr>
                    <tr>
                        <td>Λειτουργικό Σύστημα:</td>
                        <td>{this.props.product.OS}</td>
                    </tr>
                    <tr>
                        <td>Μέγεθος Οθόνης:</td>
                        <td>{this.props.product.screensize}</td>
                    </tr>
                    <tr>
                        <td>Κάρτα Γραφικών:</td>
                        <td>{this.props.product.graphicscard}</td>
                    </tr>
            </div>
        )
	}
}

class ProductViewShopList extends React.Component{
    
    constructor(props){
        super(props);
        this.state = {
            display: 'none'
        };
        this.handleClick = this.handleClick.bind(this);
    }

    handleClick() {
        const newDisplay = this.state.display == 'none' ? 'map' : 'none';
        this.setState({display: newDisplay});
    }
    
    render(){
        let map;
        if (this.state.display == 'map'){
            map = <Map />;
        }
        else{
            map = <div></div>;
        }

        const shoplist = this.props.shops.map(shop => 
            <div class='shopbox'>
                <as class='shopname'>{shop.name} </as>
                <as class='price'>{shop.price} </as><br></br> 
                <as>Άλλα στοιχεία: {shop.otherData} </as><br></br>
                <as>Αξιοπιστία καταχώρησης: <img src={like} class='like-img'/> <img src={unlike} class='like-img'/><img src={questionmark} class='questionmark-img'/></as><br></br>
                <button href='' class='address' onClick={this.handleClick}>Αλέκου Παναγούλη 9, Νίκαια </button><br></br>
                <div>{map}</div>
            </div>
            );
        return(
            <div>
                <h3>Διαθέσιμο στα εξής Καταστήματα:</h3>
                <div class='myshoplist'>{shoplist}</div>
            </div>
        )
    }
}
