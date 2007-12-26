package org.opennms.netmgt.ticketer.quickbase;

import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import junit.framework.TestCase;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import com.intuit.quickbase.util.QuickBaseClient;
import com.intuit.quickbase.util.QuickBaseException;

public class QuickBaseAPITest extends TestCase {
    
    public void testCreateLead() {
        String className = "QuickBaseAPITEst";
        
        PrintWriter out = null;
        try {
            out = new PrintWriter(System.out);
            out.println("Welcome to QuickBase\n");

            QuickBaseClient qdb = createClient();

            HashMap tables = (HashMap)qdb.grantedDBs(false, false, true);
            if(tables == null) {
              out.println("No tables belong to this user.");
            }
            Set tableNames = tables.keySet();
            String tableName = "";
            String tableDbid = "";
            for (Iterator it = tableNames.iterator(); it.hasNext();){
                  tableName = (String)it.next();
                  tableDbid = (String)tables.get(tableName);
                  out.println("Name: " + tableName + " DBID: " + tableDbid);
            Document schema = qdb.getSchema(tableDbid);
            
            Transformer transformer = TransformerFactory.newInstance().newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.transform(new DOMSource(schema), new StreamResult(out));
                                     
            
            out.println(schema);
            NodeList fields = schema.getElementsByTagName("field");
            out.println("The QuickBase application " + tableName + " has " + fields.getLength() + " fields.");
            out.println("The fields are listed below.");

            for (int i = 0; i < fields.getLength(); i++) {
              out.println("Field ID: " + fields.item(i).getAttributes().getNamedItem("id").getNodeValue());
              out.println("Field Type: " + fields.item(i).getAttributes().getNamedItem("type").getNodeValue());
              //out.println("Field Label: " + fields.item(i).getChildNodes().item(0).getNodeValue());
            }
            
            }            
        } catch (QuickBaseException qdbe) {
            System.err.println("Exception in main "+ qdbe.toString()+ " error code: "+qdbe.getErrorCode() );
            qdbe.printStackTrace();
        } catch (Exception e) {
            System.err.println("Exception in main "+ e.toString() );
            e.printStackTrace();

        } finally {
            if (null != out) {
                out.flush();    
                out.close();
            }
        }
        
    }
    
    
    private QuickBaseClient createClient() {
        return new QuickBaseClient("user@example.com", "XXXXXX");
    }
    
    public void testCreateTicket() {
    }
    
    public void testUpdateTicket() {
  
        
 
    }

}
