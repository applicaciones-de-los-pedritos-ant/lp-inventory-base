/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package org.rmj.cas.inventory.production.base;

import com.mysql.jdbc.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import javax.sql.rowset.CachedRowSet;
import javax.sql.rowset.RowSetFactory;
import javax.sql.rowset.RowSetProvider;
import org.json.simple.JSONObject;
import org.rmj.appdriver.GRider;
import org.rmj.appdriver.MiscUtil;
import org.rmj.appdriver.SQLUtil;
import org.rmj.appdriver.agentfx.CommonUtils;
import org.rmj.appdriver.agentfx.ui.showFXDialog;
import org.rmj.appdriver.constants.EditMode;
import org.rmj.appdriver.constants.TransactionStatus;
import org.rmj.appdriver.constants.UserRight;
import org.rmj.cas.inventory.others.pojo.UnitDailyProductionDetailOthers;
import org.rmj.cas.inventory.production.pojo.UnitDailyProductionDetail;

/**
 *
 * @author User
 */
public class ProductionRequest {

    private String MASTER_TABLE = "Product_Request_Master";
    private String DETAIL_TABLE = "Product_Request_Detail";
    private final GRider p_oApp;
    private final boolean p_bWithParent;

    private String p_sBranchCd = "";
    private int p_nEditMode;
    private int p_nTranStat;

    private String p_sMessage;
    private boolean p_bWithUI = true;

    private CachedRowSet p_oMaster;
    private CachedRowSet p_oMasterDetail;
    private CachedRowSet p_oDetail;

    private LMasDetTrans p_oListener;

    public ProductionRequest(GRider foApp, String fsBranchCd, boolean fbWithParent) {
        p_oApp = foApp;
        p_sBranchCd = fsBranchCd;
        p_bWithParent = fbWithParent;

        if (p_sBranchCd.isEmpty()) {
            p_sBranchCd = p_oApp.getBranchCode();
        }

        p_nTranStat = 0;
        p_nEditMode = EditMode.UNKNOWN;
    }

    public void setTranStat(int fnValue) {
        p_nTranStat = fnValue;
    }

    public void setListener(LMasDetTrans foValue) {
        p_oListener = foValue;
    }

    public void setWithUI(boolean fbValue) {
        p_bWithUI = fbValue;
    }

    public int getEditMode() {
        return p_nEditMode;
    }

    public String getMessage() {
        return p_sMessage;
    }

    public Object getMaster(int fnIndex) throws SQLException {
        if (fnIndex == 0) {
            return null;
        }

        p_oMaster.first();
        return p_oMaster.getObject(fnIndex);
    }

    public Object getMaster(String fsIndex) throws SQLException {
        return getMaster(getColumnIndex(p_oMaster, fsIndex));
    }

    public void setMaster(int fnIndex, Object foValue) throws SQLException {
        if (p_nEditMode != EditMode.ADDNEW && p_nEditMode != EditMode.UPDATE) {
            System.out.println("Invalid Edit Mode Detected.");
            return;
        }

        p_oMaster.first();

        switch (fnIndex) {
            case 2://dTransact
            case 6://sReqstdBy
            case 9://dModified
                if (foValue instanceof Date) {
                    p_oMaster.updateDate(fnIndex, SQLUtil.toDate((Date) foValue));
                } else {
                    p_oMaster.updateDate(fnIndex, SQLUtil.toDate(p_oApp.getServerDate()));
                }

                p_oMaster.updateRow();

                if (p_oListener != null) {
                    p_oListener.MasterRetreive(fnIndex, p_oMaster.getString(fnIndex));
                }
                break;
            case 3://nEntryNox
            case 7://cTranStat
                if (foValue instanceof Integer) {
                    p_oMaster.updateInt(fnIndex, (int) foValue);
                } else {
                    p_oMaster.updateInt(fnIndex, 0);
                }

                p_oMaster.updateRow();
                if (p_oListener != null) {
                    p_oListener.MasterRetreive(fnIndex, p_oMaster.getString(fnIndex));
                }
                break;
            default:
                p_oMaster.updateString(fnIndex, (String) foValue);
                p_oMaster.updateRow();
                if (p_oListener != null) {
                    p_oListener.MasterRetreive(fnIndex, p_oMaster.getString(fnIndex));
                }
                break;
        }
    }

    public void setMaster(String fsIndex, Object foValue) throws SQLException {
        setMaster(getColumnIndex(p_oMaster, fsIndex), foValue);
    }

    // Product request tab dashboard
    public int getMasterItemCount() throws SQLException {
        if (p_oMasterDetail == null) {
            return 0;
        }
        p_oMasterDetail.last();
        return p_oMasterDetail.getRow();
    }

    public Object getMasterDetail(int fnRow, int fnIndex) throws SQLException {
        if (getMasterItemCount() == 0) {
            return null;
        }

        if (getMasterItemCount() == 0 || fnRow > getMasterItemCount()) {
            return null;
        }

        p_oMasterDetail.absolute(fnRow);
        return p_oMasterDetail.getObject(fnIndex);

    }

    public Object getMasterDetail(int fnRow, String fsIndex) throws SQLException {
        return getMasterDetail(fnRow, getColumnIndex(p_oMasterDetail, fsIndex));
    }

    public void setMasterDetail(int fnRow, String fsIndex, Object foValue) throws SQLException {
        setMasterDetail(fnRow, getColumnIndex(p_oMasterDetail, fsIndex), foValue);
    }

    public void setMasterDetail(int fnRow, int fnIndex, Object foValue) throws SQLException {
        if (p_nEditMode != EditMode.ADDNEW && p_nEditMode != EditMode.UPDATE) {
            System.out.println("Invalid Edit Mode Detected.");
            return;
        }
        p_oMasterDetail.absolute(fnRow);
        p_oMasterDetail.updateString(fnIndex, (String) foValue);
        p_oMasterDetail.updateRow();
    }

    public int getItemCount() throws SQLException {
        if (p_oDetail == null) {
            return 0;
        }
        p_oDetail.last();
        return p_oDetail.getRow();
    }

    public Object getDetail(int fnRow, int fnIndex) throws SQLException {
        if (getItemCount() == 0) {
            return null;
        }

        if (getItemCount() == 0 || fnRow > getItemCount()) {
            return null;
        }

        p_oDetail.absolute(fnRow);
        return p_oDetail.getObject(fnIndex);

    }

    public Object getDetail(int fnRow, String fsIndex) throws SQLException {
        return getDetail(fnRow, getColumnIndex(p_oDetail, fsIndex));
    }

    public void setDetail(int fnRow, String fsIndex, Object foValue) throws SQLException {
        setDetail(fnRow, getColumnIndex(p_oDetail, fsIndex), foValue);
    }

    public void setDetail(int fnRow, int fnIndex, Object foValue) throws SQLException {
        if (p_nEditMode != EditMode.ADDNEW && p_nEditMode != EditMode.UPDATE) {
            System.out.println("Invalid Edit Mode Detected.");
            return;
        }
        //p_oPayment.first();
        p_oDetail.absolute(fnRow);

        switch (fnIndex) {
            case 3://nQuantity
            case 4://nEntryNox
            case 5://nQtyOnHnd
                if (foValue instanceof Number) {
                    p_oDetail.updateObject(fnIndex,  foValue);
                    p_oDetail.updateRow();
                }

                if (p_oListener != null) {
                    p_oListener.DetailRetreive(fnRow, fnIndex, p_oDetail.getObject(fnIndex));
                }
                break;
            case 1: //sTransNox
            case 2: //sStockIDx
            case 6: //sDescript
            case 7: //xBrandNme
            case 8: //xModelNme
            case 9: //xInvTypNm
            case 10: //sMeasurNm
                p_oDetail.updateString(fnIndex, (String) foValue);
                p_oDetail.updateRow();
                if (p_oListener != null) {
                    p_oListener.DetailRetreive(fnRow, fnIndex, p_oDetail.getString(fnIndex));
                }
                break;
        }
    }

    public boolean NewRecord() throws SQLException {
        if (p_oApp == null) {
            p_sMessage = "Application driver is not set.";
            return false;
        }

        p_sMessage = "";

        String lsSQL = MiscUtil.addCondition(getSQL_Master(), "0=1");
        ResultSet loRS = p_oApp.executeQuery(lsSQL);

        RowSetFactory factory = RowSetProvider.newFactory();
        p_oMaster = factory.createCachedRowSet();
        p_oMaster.populate(loRS);
        MiscUtil.close(loRS);

        p_oMaster.last();
        p_oMaster.moveToInsertRow();

        MiscUtil.initRowSet(p_oMaster);

        p_oMaster.updateObject("sTransNox", MiscUtil.getNextCode(MASTER_TABLE, "sTransNox", true, p_oApp.getConnection(), p_sBranchCd));
        p_oMaster.updateObject("dTransact", p_oApp.getServerDate());
        p_oMaster.updateObject("nEntryNox", 1);
        p_oMaster.updateObject("cTranStat", TransactionStatus.STATE_OPEN);

        p_oMaster.insertRow();
        p_oMaster.moveToCurrentRow();

        addDetail();
        p_nEditMode = EditMode.ADDNEW;
        return true;
    }

    public boolean SearchBranch(String fsValue, boolean fbByCode) throws SQLException {
        if (p_oApp == null) {
            p_sMessage = "Application driver is not set.";
            return false;
        }

        p_sMessage = "";

        String lsSQL = "SELECT"
                + "  sBranchCd"
                + ", sBranchNm"
                + " FROM Branch"
                + " WHERE sBranchCd IN ('PK01', 'PR01')";

        if (p_bWithUI) {
            JSONObject loJSON = showFXDialog.jsonSearch(
                    p_oApp,
                    lsSQL,
                    fsValue,
                    "Code»Branch",
                    "sBranchCd»sBranchNm",
                    "sBranchCd»sBranchNm",
                    fbByCode ? 0 : 1);

            if (loJSON != null) {
                setMaster("sIssuingx", (String) loJSON.get("sBranchCd"));
                setMaster("sBranchNm", (String) loJSON.get("sBranchNm"));
                return true;
            } else {
                setMaster("sIssuingx", "");
                setMaster("sBranchNm", "");
                p_sMessage = "No record selected.";
                return false;
            }
        }

        if (fbByCode) {
            lsSQL = MiscUtil.addCondition(lsSQL, "sBranchCd = " + SQLUtil.toSQL(fsValue));
        } else {
            lsSQL = MiscUtil.addCondition(lsSQL, "sBranchNm LIKE " + SQLUtil.toSQL(fsValue + "%"));
            lsSQL += " LIMIT 1";
        }

        ResultSet loRS = p_oApp.executeQuery(lsSQL);

        if (!loRS.next()) {
            MiscUtil.close(loRS);
            setMaster("sIssuingx", "");
            setMaster("sBranchNm", "");
            p_sMessage = "No record found for the givern criteria.";
            return false;
        }

        setMaster("sIssuingx", loRS.getString("sBranchCd"));
        setMaster("sBranchNm", loRS.getString("sBranchNm"));
        MiscUtil.close(loRS);

        return true;
    }

    public boolean SearchRecord(String fsValue, boolean fbByCode) throws SQLException {
        if (p_oApp == null) {
            p_sMessage = "Application driver is not set.";
            return false;
        }

        p_sMessage = "";

        String lsSQL = getSQ_BrowseProducton();
        String lsCondition = "";

        if (fbByCode) {
            lsSQL = MiscUtil.addCondition(lsSQL, "a.sTransNox LIKE " + SQLUtil.toSQL(fsValue + "%"));
        } 

        System.out.println(lsSQL);
        if (p_bWithUI) {
            JSONObject loJSON = showFXDialog.jsonSearch(
                    p_oApp,
                    lsSQL,
                    fsValue,
                    "Trans. No.»Date Transact»Requested By»Remarks",
                    "a.sTransNox»dTransact»c.sClientNm»a.sRemarksx",
                    "a.sTransNox»dTransact»c.sClientNm»a.sRemarksx",
                    fbByCode ? 0 : 1);

            if (loJSON != null) {
                return OpenRecord((String) loJSON.get("sTransNox"));
            } else {
                p_sMessage = "No record selected.";
                return false;
            }
        }

        if (fbByCode) {
            lsSQL = MiscUtil.addCondition(lsSQL, "a.sTransNox = " + SQLUtil.toSQL(fsValue));
        } else {
            lsSQL = MiscUtil.addCondition(lsSQL, "a.sTransNox LIKE " + SQLUtil.toSQL(fsValue + "%"));
            lsSQL += " LIMIT 1";
        }

        ResultSet loRS = p_oApp.executeQuery(lsSQL);

        if (!loRS.next()) {
            MiscUtil.close(loRS);
            p_sMessage = "No transaction found for the givern criteria.";
            return false;
        }

        lsSQL = loRS.getString("sTransNox");
        MiscUtil.close(loRS);

        return OpenRecord(lsSQL);
    }

        public boolean OpenRecord(String fsValue) throws SQLException {
        p_nEditMode = EditMode.UNKNOWN;

        if (p_oApp == null) {
            p_sMessage = "Application driver is not set.";
            return false;
        }

        p_sMessage = "";

        String lsSQL;
        ResultSet loRS;
        RowSetFactory factory = RowSetProvider.newFactory();

        //open master
        lsSQL = MiscUtil.addCondition(getSQL_Master(), "a.sTransNox = " + SQLUtil.toSQL(fsValue));
        loRS = p_oApp.executeQuery(lsSQL);
        p_oMaster = factory.createCachedRowSet();
        p_oMaster.populate(loRS);
        MiscUtil.close(loRS);

        //open detail
        lsSQL = MiscUtil.addCondition(getSQL_Detail(), "a.sTransNox = " + SQLUtil.toSQL(fsValue));
        loRS = p_oApp.executeQuery(lsSQL);
        p_oDetail = factory.createCachedRowSet();
        p_oDetail.populate(loRS);
        MiscUtil.close(loRS);

        p_nEditMode = EditMode.READY;
        return true;
    }

    public boolean LoadList() throws SQLException {
        p_nEditMode = EditMode.UNKNOWN;

        if (p_oApp == null) {
            p_sMessage = "Application driver is not set.";
            return false;
        }

        p_sMessage = "";

        String lsSQL;
        ResultSet loRS;
        RowSetFactory factory = RowSetProvider.newFactory();

        //open master
        lsSQL = getSQL_Master();
        loRS = p_oApp.executeQuery(lsSQL);
        p_oMasterDetail = factory.createCachedRowSet();
        p_oMasterDetail.populate(loRS);
        MiscUtil.close(loRS);

        p_nEditMode = EditMode.READY;
        return true;
    }

    public boolean UpdateTransaction() throws SQLException {
        if (p_oApp == null) {
            p_sMessage = "Application driver is not set.";
            return false;
        }

        if (p_nEditMode != EditMode.READY) {
            p_sMessage = "Invalid edit mode.";
            return false;
        }

        if (p_bWithParent) {
            p_sMessage = "Updating of record from other object is not allowed.";
            return false;
        }

        if (Integer.parseInt((String) getMaster("cTranStat")) > 2) {
            p_sMessage = "Unable to update processed transactions.";
            return false;
        }

        p_nEditMode = EditMode.UPDATE;
        return true;
    }

    public boolean SearchRequestedBy() {
        return true;
    }

    public boolean SearchDetail(int fnRow, int fnCol, String fsValue, boolean fbByCode) throws SQLException {
        if (p_nEditMode != EditMode.ADDNEW && p_nEditMode != EditMode.UPDATE) {
            return false;
        }

        ResultSet loRS;
        String lsSQL = getSQ_Stocks();
        System.out.println(fnRow);
        switch (fnCol) {
            case 1:
                return SearchBarrcode(fnRow, fnCol, fsValue);
            case 2:
                return SearchDescript(fnRow, fnCol, fsValue);
        }
        return true;
    }

    private boolean SearchBarrcode(int fnRow, int fnCol, String fsValue) throws SQLException {
        ResultSet loRS;
        String lsSQL = getSQ_Stocks();

        JSONObject loJSON;
        p_oDetail.absolute(fnRow);

        if (p_bWithUI) {
//            loJSON = showFXDialog.jsonSearch(p_oApp,
//                     lsSQL,
//                     fsValue,
//                     "Barcode»Description»Inv. Type»Brand»Model»Stock ID",
//                     "sBarCodex»sDescript»xInvTypNm»xBrandNme»xModelNme»sStockIDx",
//                     "a.sBarCodex»a.sDescript»d.sDescript»b.sDescript»c.sDescript»a.sStockIDx",
//                     0);

            loJSON = showFXDialog.jsonSearch(p_oApp,
                    lsSQL,
                    fsValue,
                    "Barcode»Description»Brand»Unit»Qty on Hand»Stock ID»Inv. Type»Model",
                    "a.sBarCodex»a.sDescript»xBrandNme»f.sMeasurNm»e.nQtyOnHnd»sStockIDx»xInvTypNm»xModelNme",
                    "a.sBarCodex»a.sDescript»b.sDescript»f.sMeasurNm»e.nQtyOnHnd»a.sStockIDx»d.sDescript»c.sDescript",
                    0);

            if (loJSON != null) {
                p_oDetail.updateObject("sStockIDx", (String) loJSON.get("sStockIDx"));
                p_oDetail.updateObject("sBarCodex", (String) loJSON.get("sBarCodex"));
                p_oDetail.updateObject("nQtyOnHnd", (String) loJSON.get("nQtyOnHnd"));
                p_oDetail.updateObject("sDescript", (String) loJSON.get("sDescript"));
                p_oDetail.updateObject("xBrandNme", (String) loJSON.get("xBrandNme"));
                p_oDetail.updateObject("xModelNme", (String) loJSON.get("xModelNme"));
                p_oDetail.updateObject("xInvTypNm", (String) loJSON.get("xInvTypNm"));

                return true;
            } else {
                p_sMessage = "No record selected.";
                return false;
            }
        }

        lsSQL = MiscUtil.addCondition(lsSQL, "a.sBarCodex LIKE " + SQLUtil.toSQL("%" + fsValue));
        lsSQL += " LIMIT 1";

        loRS = p_oApp.executeQuery(lsSQL);

        if (!loRS.next()) {
            MiscUtil.close(loRS);
            p_sMessage = "No bracnh found for the givern criteria.";
            return false;
        }

        lsSQL = loRS.getString("sStockIDx");
        if (lsSQL != null) {
            System.out.println(loRS.getString("sBrandNme"));
            p_oDetail.updateString("sStockIDx", loRS.getString("sStockIDx"));
            p_oDetail.updateString("sBarCodex", loRS.getString("sBarCodex"));
            p_oDetail.updateObject("nQtyOnHnd", loRS.getObject("nQtyOnHnd"));
            p_oDetail.updateString("sDescript", loRS.getString("sDescript"));
            p_oDetail.updateString("xBrandNme", loRS.getString("xBrandNme"));
            p_oDetail.updateString("xModelNme", loRS.getString("xModelNme"));
            p_oDetail.updateString("xInvTypNm", loRS.getString("xInvTypNm"));
        }
        MiscUtil.close(loRS);
        return true;
    }

    private boolean SearchDescript(int fnRow, int fnCol, String fsValue) throws SQLException {
        ResultSet loRS;
        String lsSQL = getSQ_Stocks();

        JSONObject loJSON;
        p_oDetail.absolute(fnRow);
        if (p_bWithUI) {
//            loJSON = showFXDialog.jsonSearch(
//                p_oApp, 
//                lsSQL, 
//                fsValue, 
//                "Barcode»Description»Inv. Type»Brand»Model»Stock ID", 
//                "sBarCodex»sDescript»xInvTypNm»xBrandNme»xModelNme»sStockIDx", 
//                "a.sBarCodex»a.sDescript»d.sDescript»b.sDescript»c.sDescript»a.sStockIDx", 
//                1);

            loJSON = showFXDialog.jsonSearch(p_oApp,
                    lsSQL,
                    fsValue,
                    "Barcode»Description»Brand»Unit»Qty on Hand»Stock ID»Inv. Type»Model",
                    "a.sBarCodex»a.sDescript»xBrandNme»f.sMeasurNm»e.nQtyOnHnd»sStockIDx»xInvTypNm»xModelNme",
                    "a.sBarCodex»a.sDescript»b.sDescript»f.sMeasurNm»e.nQtyOnHnd»a.sStockIDx»d.sDescript»»c.sDescript",
                    1);

            if (loJSON != null) {
                p_oDetail.updateObject("sStockIDx", (String) loJSON.get("sStockIDx"));
                p_oDetail.updateObject("sBarCodex", (String) loJSON.get("sBarCodex"));
                p_oDetail.updateObject("nQtyOnHnd", (String) loJSON.get("nQtyOnHnd"));
                p_oDetail.updateObject("sDescript", (String) loJSON.get("sDescript"));
                p_oDetail.updateObject("xBrandNme", (String) loJSON.get("xBrandNme"));
                p_oDetail.updateObject("xModelNme", (String) loJSON.get("xModelNme"));
                p_oDetail.updateObject("xInvTypNm", (String) loJSON.get("xInvTypNm"));

                return true;
            } else {
                p_sMessage = "No record selected.";
                return false;
            }
        }

        lsSQL = MiscUtil.addCondition(lsSQL, "a.sDescript LIKE " + SQLUtil.toSQL(fsValue + "%"));
        lsSQL += " LIMIT 1";

        loRS = p_oApp.executeQuery(lsSQL);

        if (!loRS.next()) {
            MiscUtil.close(loRS);
            p_sMessage = "No bracnh found for the givern criteria.";
            return false;
        }

        lsSQL = loRS.getString("sStockIDx");

        if (lsSQL != null) {
            System.out.println(loRS.getString("sBrandNme"));
            p_oDetail.updateString("sStockIDx", loRS.getString("sStockIDx"));
            p_oDetail.updateString("sBarCodex", loRS.getString("sBarCodex"));
            p_oDetail.updateString("nQtyOnHnd", loRS.getString("nQtyOnHnd"));
            p_oDetail.updateString("sDescript", loRS.getString("sDescript"));
            p_oDetail.updateString("xBrandNme", loRS.getString("xBrandNme"));
            p_oDetail.updateString("xModelNme", loRS.getString("xModelNme"));
            p_oDetail.updateString("xInvTypNm", loRS.getString("xInvTypNm"));
        }

        MiscUtil.close(loRS);
        return true;
    }

    public boolean addDetail() throws SQLException {

        String lsSQL = MiscUtil.addCondition(getSQL_Detail(), "0=1");
        ResultSet loRS = p_oApp.executeQuery(lsSQL);
        RowSetFactory factory = RowSetProvider.newFactory();

        p_oDetail = factory.createCachedRowSet();
        p_oDetail.populate(loRS);
        MiscUtil.close(loRS);
        p_oDetail.last();
        p_oDetail.moveToInsertRow();

        MiscUtil.initRowSet(p_oDetail);

        p_oDetail.updateObject("sTransNox", MiscUtil.getNextCode(MASTER_TABLE, "sTransNox", true, p_oApp.getConnection(), p_sBranchCd));
        p_oDetail.updateObject("nQuantity", 0);
        p_oDetail.updateObject("nQtyOnHnd", 0);
        p_oDetail.updateObject("sStockIDx", "");
        p_oDetail.updateObject("nEntryNox", 1);

        p_oDetail.insertRow();
        p_oDetail.moveToCurrentRow();

        return true;
    }

    public boolean addNewDetail() throws SQLException {
        if (p_oDetail == null) {
            p_sMessage = "Detail row set is not initialized.";
            return false;
        }

        if (p_oDetail.last()) {
            String lastStockID = p_oDetail.getString("sStockIDx");
            if (lastStockID == null || lastStockID.isEmpty()) {
                p_sMessage = "The last row's Stock ID is invalid. Cannot add a new detail.";
                return false;
            }
        }
        p_oDetail.moveToInsertRow();

        MiscUtil.initRowSet(p_oDetail);

        p_oDetail.updateObject("sTransNox", MiscUtil.getNextCode(MASTER_TABLE, "sTransNox", true, p_oApp.getConnection(), p_sBranchCd));
        p_oDetail.updateObject("nQuantity", 0);
        p_oDetail.updateObject("nQtyOnHnd", 0);
        p_oDetail.updateObject("sStockIDx", ""); // Set to empty string
        p_oDetail.insertRow();
        p_oDetail.moveToCurrentRow();

        return true;
    }

    public boolean deleteDetail(int fnRow) throws SQLException {
        p_oDetail.beforeFirst();

        // Loop through the rows
        while (p_oDetail.next()) {
            // Get the position of the current row
            int position = p_oDetail.getRow();
            if (position == fnRow) {
                p_oDetail.deleteRow();
            }

            // Do some other operation on the row
        }

        if (getItemCount() <= 0) {
            addDetail();
        }

        return true;
    }

    public boolean SaveRecord() throws SQLException {
        if (p_oApp == null) {
            p_sMessage = "Application driver is not set.";
            return false;
        }

        p_sMessage = "";

        if (p_nEditMode != EditMode.ADDNEW
                && p_nEditMode != EditMode.UPDATE) {
            p_sMessage = "Invalid edit mode detected.";
            return false;
        }

        if (!isEntryOK()) {
            return false;
        }

        int lnCtr;
        String lsSQL;

        if (p_nEditMode == EditMode.ADDNEW) {
            if (!p_bWithParent) {
                p_oApp.beginTrans();
            }

            //set transaction number on records
            String lsTransNox = MiscUtil.getNextCode(MASTER_TABLE, "sTransNox", true, p_oApp.getConnection(), p_sBranchCd);
            p_oMaster.updateObject("sTransNox", lsTransNox);
            p_oMaster.updateObject("dTransact", p_oApp.getServerDate());
            p_oMaster.updateObject("dReqstdxx", p_oApp.getServerDate());
            p_oMaster.updateObject("sReqstdBy", p_oApp.getEmployeeNo());
            p_oMaster.updateObject("dModified", p_oApp.getServerDate());
            p_oMaster.updateObject("sModified", p_oApp.getUserID());

            lnCtr = 1;
            p_oDetail.beforeFirst();
            while (p_oDetail.next()) {
                p_oDetail.updateObject("sTransNox", lsTransNox);
                p_oDetail.updateObject("nEntryNox", lnCtr);
                p_oDetail.updateRow();
                System.out.println(p_oDetail.getString("sStockIDx"));
                if (!p_oDetail.getString("sStockIDx").isEmpty()) {
                    lsSQL = MiscUtil.rowset2SQL(p_oDetail, DETAIL_TABLE, "sBarCodex;sDescript;xBrandNme;xModelNme;xInvTypNm;sMeasurNm;nQtyOnHnd");

                    if (p_oApp.executeQuery(lsSQL, DETAIL_TABLE, p_sBranchCd, lsTransNox.substring(0, 4)) <= 0) {
                        if (!p_bWithParent) {
                            p_oApp.rollbackTrans();
                        }
                        p_sMessage = p_oApp.getMessage() + " ; " + p_oApp.getErrMsg();
                        return false;
                    }

                }

                lnCtr++;
            }
            p_oMaster.updateObject("nEntryNox", lnCtr - 1);
            p_oMaster.updateRow();

            lsSQL = MiscUtil.rowset2SQL(p_oMaster, MASTER_TABLE, "sClientNm;sBranchNm");

            if (p_oApp.executeQuery(lsSQL, MASTER_TABLE, p_sBranchCd, lsTransNox.substring(0, 4)) <= 0) {
                if (!p_bWithParent) {
                    p_oApp.rollbackTrans();
                }
                p_sMessage = p_oApp.getMessage() + ";" + p_oApp.getErrMsg();
                return false;
            }

            if (!p_bWithParent) {
                p_oApp.commitTrans();
            }

            p_nEditMode = EditMode.UNKNOWN;
            return true;
        } else {
            if (!p_bWithParent) {
                p_oApp.beginTrans();
            }

            //set transaction number on records
            String lsTransNox = (String) getMaster("sTransNox");

            CachedRowSet laSubUnit = loadTransactionDetail(lsTransNox);
            lnCtr = 1;
            System.out.println(" laSubUnit = " + laSubUnit.size());
            p_oDetail.beforeFirst();
            while (p_oDetail.next()) {
                if (!p_oDetail.getString("sStockIDx").isEmpty()) {
                    if (lnCtr <= laSubUnit.size()) {
                        lsSQL = MiscUtil.rowset2SQL(p_oDetail,
                                DETAIL_TABLE,
                                "sBarCodex;sDescript;xBrandNme;xModelNme;xInvTypNm;sMeasurNm;nQtyOnHnd",
                                "sTransNox = " + SQLUtil.toSQL(lsTransNox)
                                + " AND nEntryNox = " + p_oDetail.getInt("nEntryNox"));
                    } else {
                        p_oDetail.updateObject("sTransNox", lsTransNox);
                        p_oDetail.updateObject("nEntryNox", lnCtr);
                        p_oDetail.updateRow();
                        lsSQL = MiscUtil.rowset2SQL(p_oDetail, DETAIL_TABLE, "sBarCodex;sDescript;xBrandNme;xModelNme;xInvTypNm;sMeasurNm;nQtyOnHnd");

                    }

                    if (!lsSQL.isEmpty()) {
                        if (p_oApp.executeQuery(lsSQL, DETAIL_TABLE, p_sBranchCd, lsTransNox.substring(0, 4)) <= 0) {
                            if (!p_bWithParent) {
                                p_oApp.rollbackTrans();
                            }
                            p_sMessage = p_oApp.getMessage() + ";" + p_oApp.getErrMsg();
                            return false;
                        }
                    }
                }

                lnCtr++;
            }

            p_oMaster.updateObject("nEntryNox", lnCtr - 1);
            p_oMaster.updateRow();

            lsSQL = MiscUtil.rowset2SQL(p_oMaster,
                    MASTER_TABLE,
                    "sClientNm;sBranchNm",
                    "sTransNox = " + SQLUtil.toSQL(lsTransNox));

            if (!lsSQL.isEmpty()) {
                if (p_oApp.executeQuery(lsSQL, MASTER_TABLE, p_sBranchCd, lsTransNox.substring(0, 4)) <= 0) {
                    if (!p_bWithParent) {
                        p_oApp.rollbackTrans();
                    }
                    p_sMessage = p_oApp.getMessage() + ";" + p_oApp.getErrMsg();
                    return false;
                }
            }

            if (!p_bWithParent) {
                p_oApp.commitTrans();
            }

            p_nEditMode = EditMode.UNKNOWN;
            return true;
        }
    }

    public boolean CloseRecord() throws SQLException {
        if (p_nEditMode != EditMode.READY) {
            p_sMessage = "Invalid update mode detected.";
            return false;
        }

        p_sMessage = "";

        if (p_bWithParent) {
            p_sMessage = "Confirming transactions from other object is not allowed.";
            return false;
        }

        if (((String) getMaster("cTranStat")).equals("1")) {
            p_sMessage = "Transaction was already cofirmed.";
            return false;
        }

//        if (((String) getMaster("cTranStat")).equals("2")) {
//            p_sMessage = "Transaction was already posted.";
//            return false;
//        }

        if (((String) getMaster("cTranStat")).equals("3")) {
            p_sMessage = "Transaction was already cancelled.";
            return false;
        }

        if (p_oApp.getUserLevel() < UserRight.SUPERVISOR) {
            p_sMessage = "User is not allowed confirming transaction.";
            return false;
        }

        String lsTransNox = (String) getMaster("sTransNox");

        String lsSQL = "UPDATE " + MASTER_TABLE + " SET"
                + " cTranStat = '1'"
                + " WHERE sTransNox = " + SQLUtil.toSQL(lsTransNox);

        if (p_oApp.executeQuery(lsSQL, MASTER_TABLE, p_sBranchCd, lsTransNox.substring(0, 4)) <= 0) {
            p_sMessage = p_oApp.getErrMsg() + "; " + p_oApp.getMessage();
            return false;
        }

        p_nEditMode = EditMode.UNKNOWN;
        return true;
    }

    public boolean PostRecord() throws SQLException {
        if (p_nEditMode != EditMode.READY) {
            p_sMessage = "Invalid update mode detected.";
            return false;
        }

        p_sMessage = "";
        p_oMaster.first();
        if (((String) getMaster("cTranStat")).equals("2")) {
            p_sMessage = "Transaction was already posted..";
            return false;
        }

        if (((String) getMaster("cTranStat")).equals("3")) {
            p_sMessage = "Unable to post cancelled transactions.";
            return false;
        }

        String lsTransNox = (String) getMaster("sTransNox");
        String lsSQL = "UPDATE " + MASTER_TABLE + " SET"
                + "  cTranStat = '2'"
                + " WHERE sTransNox = " + SQLUtil.toSQL(lsTransNox);

        if (p_oApp.executeQuery(lsSQL, MASTER_TABLE, p_sBranchCd, lsTransNox.substring(0, 4)) <= 0) {
            p_sMessage = p_oApp.getErrMsg() + "; " + p_oApp.getMessage();
            return false;
        }

        p_nEditMode = EditMode.UNKNOWN;
        return true;
    }

    public boolean CancelRecord() throws SQLException {
        if (p_nEditMode != EditMode.READY) {
            p_sMessage = "Invalid update mode detected.";
            return false;
        }

        p_sMessage = "";

        if (p_bWithParent) {
            p_sMessage = "Cancelling transactions from other object is not allowed.";
            return false;
        }

        if (((String) getMaster("cTranStat")).equals("2")) {
            p_sMessage = "Unable to cancel posted transactions.";
            return false;
        }

        if (((String) getMaster("cTranStat")).equals("3")) {
            p_sMessage = "Transaction was already cancelled.";
            return false;
        }

        String lsTransNox = (String) getMaster("sTransNox");
        String lsSQL = "UPDATE " + MASTER_TABLE + " SET"
                + " cTranStat = '3'"
                + " WHERE sTransNox = " + SQLUtil.toSQL(lsTransNox);

        if (p_oApp.executeQuery(lsSQL, MASTER_TABLE, p_sBranchCd, lsTransNox.substring(0, 4)) <= 0) {
            p_sMessage = p_oApp.getErrMsg() + "; " + p_oApp.getMessage();
            return false;
        }

        p_nEditMode = EditMode.UNKNOWN;
        return true;
    }

    private boolean isEntryOK() throws SQLException {
        //validate detail
        if (getItemCount() == 0) {
            p_sMessage = "No Item detail detected.";
            return false;
        }

        p_oMaster.first();
//        if (((String) p_oMaster.getString("sIssuingx")).isEmpty()) {
//            p_sMessage = "Issuing entity is not set.";
//            return false;
//        }

        p_oDetail.beforeFirst();
        while (p_oDetail.next()) {
            if (!p_oDetail.getString("sStockIDx").isEmpty()) {
                if ((Double) p_oDetail.getObject("nQuantity") <= 0) {
                    p_sMessage = "Quantity for " + p_oDetail.getString("sStockIDx") + " must not be empty.";
                    return false;
                }
            }

        }

        return true;
    }

    private CachedRowSet loadTransactionDetail(String fsTransNox) throws SQLException {
        CachedRowSet foDetail;
        String lsSQL;

        ResultSet loRS;
        RowSetFactory factory = RowSetProvider.newFactory();

        //open detail
        lsSQL = MiscUtil.addCondition(getSQL_Detail(), "sTransNox = " + SQLUtil.toSQL(fsTransNox));
        loRS = p_oApp.executeQuery(lsSQL);
        foDetail = factory.createCachedRowSet();
        foDetail.populate(loRS);
        MiscUtil.close(loRS);
        return foDetail;
    }

    private String getSQ_BrowseProducton() {
        String lsSQL = "";
        String lsCondition = "";
        String lsStat = String.valueOf(p_nTranStat);

        if (lsStat.length() > 1) {
            for (int lnCtr = 0; lnCtr <= lsStat.length() - 1; lnCtr++) {
                lsSQL += ", " + SQLUtil.toSQL(Character.toString(lsStat.charAt(lnCtr)));
            }
            lsCondition = "a.cTranStat IN (" + lsSQL.substring(2) + ")";
        } else {
            lsCondition = "a.cTranStat = " + SQLUtil.toSQL(lsStat);
        }

        lsSQL = "SELECT "
                + "  a.sTransNox"
                + ",DATE_FORMAT(a.dTransact, '%m/%d/%Y') AS dTransact"
                + ", a.sRemarksx"
                + ", c.sClientNm"
                + " FROM " + MASTER_TABLE + " a"
                + " LEFT JOIN Branch d ON a.sIssuingx = d.sBranchCd"
                + ", Employee_Master001 b "
                + " LEFT JOIN Client_Master c ON b.sEmployID = c.sClientID"
                + " WHERE a.sReqstdBy = b.sEmployID";

        lsSQL = MiscUtil.addCondition(lsSQL, lsCondition);

//        if (!"PK01;PR01;P0W2".contains(p_sBranchCd)) {
//            lsSQL = MiscUtil.addCondition(lsSQL, "a.sTransNox LIKE " + SQLUtil.toSQL(p_sBranchCd + "%"));
//        }
        return lsSQL;
    }

    private String getSQL_Master() {
        String lsSQL = "";

        String lsCondition = "";
        String lsStat = String.valueOf(p_nTranStat);

        if (lsStat.length() > 1) {
            for (int lnCtr = 0; lnCtr <= lsStat.length() - 1; lnCtr++) {
                lsSQL += ", " + SQLUtil.toSQL(Character.toString(lsStat.charAt(lnCtr)));
            }
            lsCondition = "a.cTranStat IN (" + lsSQL.substring(2) + ")";
        } else {
            lsCondition = "a.cTranStat = " + SQLUtil.toSQL(lsStat);
        }

        lsSQL = "SELECT "
                + "  a.sTransNox"
                + ", a.dTransact"
                + ", a.nEntryNox"
                + ", a.sRemarksx"
                + ", a.sReqstdBy"
                + ", a.dReqstdxx"
                + ", a.cTranStat"
                + ", a.sModified"
                + ", a.dModified"
                + ", c.sClientNm"
                + ", a.sIssuingx"
                + ", d.sBranchNm"
                + " FROM " + MASTER_TABLE + " a "
                + " LEFT JOIN Branch d ON a.sIssuingx = d.sBranchCd"
                + ", Employee_Master001 b "
                + " LEFT JOIN Client_Master c ON b.sEmployID = c.sClientID"
                + " WHERE a.sReqstdBy = b.sEmployID";

        lsSQL = MiscUtil.addCondition(lsSQL, lsCondition);

        if (!"PK01;PR01;P0W2".contains(p_sBranchCd)) {
            lsSQL = MiscUtil.addCondition(lsSQL, "a.sTransNox LIKE " + SQLUtil.toSQL(p_sBranchCd + "%"));
        }

        return lsSQL;
    }

    private String getSQL_Detail() {
        String lsSQL = "";

        lsSQL = "SELECT "
                + "  a.sTransNox"
                + ", a.sStockIDx"
                + ", a.nQuantity"
                + ", a.nEntryNox"
                + ", b.sBarCodex"
                + ", IFNULL(c.nQtyOnHnd, 0) nQtyOnHnd"
                + ", b.sDescript"
                + ", IFNULL(d.sDescript,'') xBrandNme"
                + ", IFNULL(e.sDescript,'') xModelNme"
                + ", IFNULL(f.sDescript,'') xInvTypNm"
                + ", g.sMeasurNm "
                + " FROM " + DETAIL_TABLE + " a "
                + ", Inventory b"
                + " LEFT JOIN Inv_Master c"
                + " ON b.sStockIDx = c.sStockIDx"
                + " AND c.sBranchCD = " + SQLUtil.toSQL(p_oApp.getBranchCode())
                + " LEFT JOIN Brand d"
                + " ON b.sBrandCde = d.sBrandCde"
                + " LEFT JOIN Model e"
                + " ON b.sModelCde = e.sModelCde"
                + " LEFT JOIN Inv_Type f"
                + " ON b.sInvTypCd = f.sInvTypCd"
                + " LEFT JOIN Measure g"
                + " ON b.sMeasurID = g.sMeasurID "
                + " WHERE a.sStockIDx = b.sStockIDx"
                + " AND f.sInvTypCd = 'FsGd'";

        return lsSQL;
    }

    private String getSQ_Stocks() {
        String lsSQL = "SELECT "
                + "  a.sStockIDx"
                + ", a.sBarCodex"
                + ", a.sDescript"
                + ", a.sBriefDsc"
                + ", a.sAltBarCd"
                + ", a.sCategCd1"
                + ", a.sCategCd2"
                + ", a.sCategCd3"
                + ", a.sCategCd4"
                + ", a.sBrandCde"
                + ", a.sModelCde"
                + ", a.sColorCde"
                + ", a.sInvTypCd"
                + ", a.nUnitPrce"
                + ", a.nSelPrice"
                + ", a.nDiscLev1"
                + ", a.nDiscLev2"
                + ", a.nDiscLev3"
                + ", a.nDealrDsc"
                + ", a.cComboInv"
                + ", a.cWthPromo"
                + ", a.cSerialze"
                + ", a.cUnitType"
                + ", a.cInvStatx"
                + ", a.sSupersed"
                + ", a.cRecdStat"
                + ", IFNULL(b.sDescript,'') xBrandNme"
                + ", IFNULL(c.sDescript,'') xModelNme"
                + ", IFNULL(d.sDescript,'') xInvTypNm"
                + ", e.nQtyOnHnd"
                + ", e.nResvOrdr"
                + ", e.nBackOrdr"
                + ", e.nFloatQty"
                + ", IFNULL(e.nLedgerNo, 0) nLedgerNo"
                + ", f.sMeasurNm"
                + " FROM Inventory a"
                + " LEFT JOIN Brand b"
                + " ON a.sBrandCde = b.sBrandCde"
                + " LEFT JOIN Model c"
                + " ON a.sModelCde = c.sModelCde"
                + " LEFT JOIN Inv_Type d"
                + " ON a.sInvTypCd = d.sInvTypCd"
                + " LEFT JOIN Measure f"
                + " ON a.sMeasurID = f.sMeasurID"
                + ", Inv_Master e"
                + " WHERE a.sStockIDx = e.sStockIDx"
                + " AND  d.sInvTypCd = 'FsGd' "
                + " AND e.sBranchCd = " + SQLUtil.toSQL(p_oApp.getBranchCode());
        //validate result based on the assigned inventory type.
        if (!System.getProperty("store.inventory.type").isEmpty()) {
            lsSQL = MiscUtil.addCondition(lsSQL, "a.sInvTypCd IN " + CommonUtils.getParameter(System.getProperty("store.inventory.type")));
        }

        return lsSQL;
    }

    private int getColumnIndex(CachedRowSet loRS, String fsValue) throws SQLException {
        int lnIndex = 0;
        int lnRow = loRS.getMetaData().getColumnCount();

        for (int lnCtr = 1; lnCtr <= lnRow; lnCtr++) {
            if (fsValue.equals(loRS.getMetaData().getColumnLabel(lnCtr))) {
                lnIndex = lnCtr;
                break;
            }
        }

        return lnIndex;
    }
}
