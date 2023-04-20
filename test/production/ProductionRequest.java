/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package production;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Properties;
import org.junit.AfterClass;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.rmj.appdriver.GProperty;
import org.rmj.appdriver.GRider;

/**
 *
 * @author User
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class ProductionRequest {
    static GRider instance = new GRider();
    static org.rmj.cas.inventory.production.base.ProductionRequest trans;
    
    public ProductionRequest() {
    }
    
    @BeforeClass
    public static void setUpClass() {        
//        if (!instance.logUser("General", "M001111122")){
//            System.err.println(instance.getMessage() + instance.getErrMsg());
//            System.exit(1);
//        }
//        
String path;
        if(System.getProperty("os.name").toLowerCase().contains("win")){
            path = "D:/GGC_Java_Systems";
        }
        else{
            path = "/srv/GGC_Java_Systems";
        }
        System.setProperty("sys.default.path.config", path);
        
        if (!loadProperties()){
            System.err.println("Unable to load config.");
            System.exit(1);
        } else System.out.println("Config file loaded successfully.");
        
        String lsProdctID;
        String lsUserIDxx;
        
        lsProdctID = System.getProperty("app.product.id");
        lsUserIDxx = System.getProperty("user.id");

        GRider poGRider = new GRider(lsProdctID);
        GProperty loProp = new GProperty("GhostRiderXP");

        if (!poGRider.loadEnv(lsProdctID)) {
            System.err.println(poGRider.getErrMsg());
            System.exit(1);
        }
        
        if (!poGRider.logUser(lsProdctID, lsUserIDxx)) {
            System.err.println(poGRider.getErrMsg());
            System.exit(1);
        }         
        
        trans = new org.rmj.cas.inventory.production.base.ProductionRequest(poGRider, poGRider.getBranchCode(), false);
        trans.setWithUI(false);
    }
    private static boolean loadProperties(){
        try {
            Properties po_props = new Properties();
            po_props.load(new FileInputStream("D:\\GGC_Java_Systems\\config\\rmj.properties"));
            
            System.setProperty("app.debug.mode", po_props.getProperty("app.debug.mode"));
            System.setProperty("user.id", po_props.getProperty("user.id"));
            System.setProperty("app.product.id", po_props.getProperty("app.product.id"));
            
            if (System.getProperty("app.product.id").equalsIgnoreCase("integsys")){
                System.setProperty("pos.clt.nm", po_props.getProperty("pos.clt.nm.integsys"));              
            } else{
                System.setProperty("pos.clt.nm", po_props.getProperty("pos.clt.nm.telecom"));         
            }
            
            System.setProperty("pos.clt.tin", po_props.getProperty("pos.clt.tin"));        
            System.setProperty("pos.clt.crm.no", po_props.getProperty("pos.clt.crm.no"));        
            System.setProperty("pos.clt.dir.ejournal", po_props.getProperty("pos.clt.dir.ejournal"));    
            
            //store info
            System.setProperty("store.inventory.type", po_props.getProperty("store.inventory.type"));
            System.setProperty("store.inventory.strict.type", po_props.getProperty("store.inventory.strict.type"));
            
            //UI
            System.setProperty("app.product.id.grider", po_props.getProperty("app.product.id.grider"));
            System.setProperty("app.product.id.general", po_props.getProperty("app.product.id.general"));
            System.setProperty("app.product.id.integsys", po_props.getProperty("app.product.id.integsys"));
            System.setProperty("app.product.id.telecom", po_props.getProperty("app.product.id.telecom"));
            
            return true;
        } catch (FileNotFoundException ex) {
            ex.printStackTrace();
            return false;
        } catch (IOException ex) {
            ex.printStackTrace();
            return false;
        }
    }   
    @AfterClass
    public static void tearDownClass() {
    }
    
    @Test
    public void test01NewRecord(){
        try {     
            if(trans.NewRecord()){
                System.out.println("success " + trans.getMaster("sTransNox"));
            }else{
                System.out.println("error " + trans.getMessage());
            }
        } catch (SQLException e) {
            e.printStackTrace();
            fail(e.getMessage());
        }   
    }
    
    @Test
    public void test02SearchDetail(){
        try {     
            if(trans.SearchDetail(1,  "P", false)){
                System.out.println("success " + trans.getDetail(1,2));
            }else{
                System.out.println("error " + trans.getMessage());
            }
        } catch (SQLException e) {
            e.printStackTrace();
            fail(e.getMessage());
        }   
    }
    @Test
    public void test03AddDetail(){
        try {     
            if(trans.addNewDetail()){
                System.out.println("count " + trans.getItemCount());
                for(int x = 1; x <= trans.getItemCount(); x++){
                    System.out.println("entryNox " + trans.getDetailI(x, "nEntryNox"));
                    
                }
            }else{
                System.out.println("error " + trans.getMessage());
            }
            
        } catch (SQLException e) {
            e.printStackTrace();
            fail(e.getMessage());
        }   
    }
    
    @Test
    public void test04SearchDetail(){
        try {     
            if(trans.SearchDetail(2,  "P00121000376", true)){
                System.out.println("success " + trans.getDetail(2,2));
            }else{
                System.out.println("error " + trans.getMessage());
            }
            if(trans.addNewDetail()){
                System.out.println("count " + trans.getItemCount());
                for(int x = 1; x <= trans.getItemCount(); x++){
                    System.out.println("entryNox " + trans.getDetailI(x, "nEntryNox"));
                    
                }
            }else{
                System.out.println("error " + trans.getMessage());
            }
              
            if(trans.SearchDetail(3,  "P00121000376", true)){
                System.out.println("success " + trans.getDetail(2,2));
            }else{
                System.out.println("error " + trans.getMessage());
            }
        } catch (SQLException e) {
            e.printStackTrace();
            fail(e.getMessage());
        }   
    }
    
    @Test
    public void test05getItemDetail(){
        try {     
            for(int x = 1; x <= trans.getItemCount(); x++){
                    System.out.println("sStockIDx " + trans.getDetailI(x, "sStockIDx"));
                    System.out.println("entryNox " + trans.getDetailI(x, "nEntryNox"));
                }
        } catch (SQLException e) {
            e.printStackTrace();
            fail(e.getMessage());
        }   
    }
    @Test
    public void test06DeleteItem(){
        try {     
            if(trans.deleteDetail(2)){
                
            }
        } catch (SQLException e) {
            e.printStackTrace();
            fail(e.getMessage());
        }   
    }
    
    @Test
    public void test07getItemDetail(){
        try {     
            for(int x = 1; x <= trans.getItemCount(); x++){
                    System.out.println("sStockIDx " + trans.getDetailI(x, "sStockIDx"));
                    System.out.println("entryNox " + trans.getDetailI(x, "nEntryNox"));
                }
        } catch (SQLException e) {
            e.printStackTrace();
            fail(e.getMessage());
        }   
    }
    @Test
    public void test08Save(){
        try {     
            if(trans.SaveRecord()){
                System.out.println("Save successfully");
            }else{
                System.out.println(trans.getMessage());
            }
        } catch (SQLException e) {
            e.printStackTrace();
            fail(e.getMessage());
        }     
    }
    
    @Test
    public void test09SearchRecord(){
        try {     
            if(trans.SearchRecord("P0W123000001", false)){
                System.out.println("Save successfully");
            }else{
                System.out.println(trans.getMessage());
            }
        } catch (SQLException e) {
            e.printStackTrace();
            fail(e.getMessage());
        }     
    }
    
}
