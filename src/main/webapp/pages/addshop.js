import React from 'react';
import NavBar from '../components/NavBar';
import Footer from '../components/Footer';
import Head from 'next/head';

class content extends React.Component {
   
    render() {
        return (
            <div id="container">               
                <Head>
                    <title> Προσθέστε κατάστημα | Stingy </title>
                    <link rel="shortcut icon" href="../static/logo/logo.png"/>    
                    <link href='../static/addrecord.css' type='text/css' rel='stylesheet' />
                </Head>
                <div id='body'>
                    <NavBar />
                    <div>
                    <h2 class='aheader'>Προσθήκη Καταστήματος:</h2>
                    <div>
                    <form class='form1'>
                        <table class='tablecontent'>
                            <tbody>
                                <tr>
                                    <td>
                                        <label>Όνομα καταστήματος:</label>                       
                                    </td>
                                    <td>
                                        <input type='text' required /> 
                                    </td>
                                </tr>
                                <tr>
                                    <td>
                                        <label>Διεύθυνση καταστήματος:</label>
                                    </td>
                                    <td>
                                        <input type='text' required />
                                    </td>
                                </tr>
                                <tr>
                                    <td>
                                        <label>Προσδιοριστής καταστήματος:</label>
                                    </td>
                                    <td>
                                        <input type='text' required />
                                    </td>
                                </tr>
                                <tr>
                                    <td>
                                        <label>Γεωγραφικό μήκος:</label>
                                    </td>
                                    <td>
                                        <input type='text' required />
                                    </td>
                                </tr>
                                <tr>
                                    <td>
                                        <label>Γεωγραφικό πλάτος:</label>
                                    </td>
                                    <td>
                                        <input type='text' required />
                                    </td>
                                </tr>
                                <tr>
                                <td>
                                    <label>Withdrawn:</label>
                                </td>
                                    <td>
                                        <input type='checkbox' />
                                    </td>
                                </tr>
                                <tr>
                
                                </tr>
                            </tbody>
                        </table>
                        <input type='button' class='button2' value='Submit' />
                    </form>
                    </div>
                    <div class='footerdiv'>
                        <Footer />
                    </div>
                    </div>
                </div>
            </div>
        );
    }
}

export default content;