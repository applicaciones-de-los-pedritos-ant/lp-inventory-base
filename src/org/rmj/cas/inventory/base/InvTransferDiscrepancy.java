/**
 * Inventory Transfer BASE
 *
 * @author Michael Torres Cuison
 * @since 2018.10.06
 */
package org.rmj.cas.inventory.base;

import com.mysql.jdbc.Connection;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.rmj.appdriver.constants.EditMode;
import org.rmj.appdriver.constants.RecordStatus;
import org.rmj.appdriver.constants.TransactionStatus;
import org.rmj.appdriver.GRider;
import org.rmj.appdriver.iface.GEntity;
import org.rmj.appdriver.MiscUtil;
import org.rmj.appdriver.SQLUtil;
import org.rmj.appdriver.agentfx.CommonUtils;
import org.rmj.appdriver.agentfx.ShowMessageFX;
import org.rmj.appdriver.agentfx.ui.showFXDialog;
import org.rmj.cas.inventory.others.pojo.UnitInvTransferDetailOthers;
import org.rmj.cas.inventory.pojo.UnitInvMaster;
import org.rmj.lp.parameter.agent.XMBranch;
import org.rmj.appdriver.agentfx.callback.IMasterDetail;
import org.rmj.cas.inventory.pojo.UnitInvTransferDiscrepancyDetail;
import org.rmj.cas.inventory.pojo.UnitInvTransferDiscrepancyMaster;

public class InvTransferDiscrepancy {

    public InvTransferDiscrepancy(GRider foGRider, String fsBranchCD, boolean fbWithParent) {
        this.poGRider = foGRider;

        if (foGRider != null) {
            this.pbWithParent = fbWithParent;
            this.psBranchCd = fsBranchCD;

            this.psUserIDxx = foGRider.getUserID();
            pnEditMode = EditMode.UNKNOWN;
        }
    }

    public boolean BrowseRecord(String fsValue, boolean fbByCode) {
        String lsHeader = "Transfer No»Destination»Date";
        String lsColName = "sTransNox»sBranchNm»dTransact";
        String lsColCrit = "a.sTransNox»b.sBranchNm»a.dTransact";
        String lsSQL = MiscUtil.addCondition(getSQ_InvTransferDiscrepancy(), "a.sBranchCd = " + SQLUtil.toSQL(psBranchCd));

        System.out.print(lsSQL);
        JSONObject loJSON = showFXDialog.jsonSearch(poGRider,
                lsSQL,
                fsValue,
                lsHeader,
                lsColName,
                lsColCrit,
                fbByCode ? 0 : 1);

        if (loJSON == null) {
            return false;
        } else {
            return openTransaction((String) loJSON.get("sTransNox"));
        }
    }

    public boolean BrowseAcceptance(String fsValue, boolean fbByCode) {
        String lsHeader = "Transfer No»Source»Date»Destination";
        String lsColName = "a.sTransNox»c.sBranchNm»dTransact»b.sBranchNm";
        String lsColCrit = "a.sTransNox»c.sBranchNm»a.dTransact»b.sBranchNm";
        String lsSQL = MiscUtil.addCondition(getSQ_InvTransferDiscrepancy(),
                "a.sDestinat = " + SQLUtil.toSQL(poGRider.getBranchCode())
                + " AND a.cTranStat = " + SQLUtil.toSQL(TransactionStatus.STATE_CLOSED));

        System.out.println(lsSQL);
        JSONObject loJSON = showFXDialog.jsonSearch(poGRider,
                lsSQL,
                fsValue,
                lsHeader,
                lsColName,
                lsColCrit,
                fbByCode ? 0 : 1);

        if (loJSON == null) {
            return false;
        } else {
            return openTransaction((String) loJSON.get("sTransNox"));
        }
    }

    public boolean addDetail() {
        if (paDetail.isEmpty()) {
            paDetail.add(new UnitInvTransferDiscrepancyDetail());
            paDetailOthers.add(new UnitInvTransferDetailOthers());
        } else {
            if (!paDetail.get(ItemCount() - 1).getStockIDx().equals("")
                    && Double.valueOf(paDetail.get(ItemCount() - 1).getQuantity().toString()) != 0.00) {
                paDetail.add(new UnitInvTransferDiscrepancyDetail());

                paDetailOthers.add(new UnitInvTransferDetailOthers());
            }

        }
        return true;
    }

    public boolean deleteDetail(int fnRow) {
        paDetail.remove(fnRow);
        paDetailOthers.remove(fnRow);
        poData.setTranTotl(computeTotal());

        if (paDetail.isEmpty()) {
            paDetail.add(new UnitInvTransferDiscrepancyDetail());
            paDetailOthers.add(new UnitInvTransferDetailOthers());
        }

        return true;
    }

    public void setDetail(int fnRow, int fnCol, Object foData) {
        if (pnEditMode != EditMode.UNKNOWN) {
            // Don't allow specific fields to assign values
            if (!(fnCol == poDetail.getColumn("sTransNox")
                    || fnCol == poDetail.getColumn("nEntryNox")
                    || fnCol == poDetail.getColumn("dModified"))) {

                if (fnCol == poDetail.getColumn("nQuantity")) {
                    if (foData instanceof Number) {
                            paDetail.get(fnRow).setValue(fnCol, foData);
                    }
                } else if (fnCol == poDetail.getColumn("nInvCostx")) {
                    if (foData instanceof Number) {
                        paDetail.get(fnRow).setValue(fnCol, foData);
                    } else {
                        paDetail.get(fnRow).setValue(fnCol, 0.00);
                    }
                } else if (fnCol == poDetail.getColumn("dExpiryDt")) {
                    if (foData instanceof Date) {
                        paDetail.get(fnRow).setValue(fnCol, foData);
                    } else {
                        paDetail.get(fnRow).setValue(fnCol, poGRider.getServerDate());
                    }
                } else {
                    paDetail.get(fnRow).setValue(fnCol, foData);
                }

                DetailRetreived(fnCol);

                poData.setTranTotl(computeTotal());
                MasterRetreived(8);
            }
        }
    }

    public void setDetail(int fnRow, String fsCol, Object foData) {
        setDetail(fnRow, poDetail.getColumn(fsCol), foData);
    }

        public boolean setItemDetail(int fnRow, int fnCol, String fsValue, boolean fbSearch, boolean fbByCode) {
        String lsHeader = "";
        String lsColName = "";
        String lsColCrit = "";
        String lsSQL = "";

        JSONObject loJSON;
        ResultSet loRS;

        setErrMsg("");
        setMessage("");

        lsSQL = MiscUtil.addCondition(getSQ_Stocks(), "a.cRecdStat = " + SQLUtil.toSQL(RecordStatus.ACTIVE));

                lsHeader = "Barcode»Description»Brand»Unit»Qty on Hand»Stock ID»Inv. Type";
                lsColName = "a.sBarCodex»a.sDescript»xBrandNme»f.sMeasurNm»e.nQtyOnHnd»sStockIDx»xInvTypNm";
                lsColCrit = "a.sBarCodex»a.sDescript»b.sDescript»f.sMeasurNm»e.nQtyOnHnd»a.sStockIDx»d.sDescript";
        if (fbByCode) {
            if (paDetailOthers.get(fnRow).getValue("sStockIDx").equals(fsValue)) {
                return true;
            }

            lsSQL = MiscUtil.addCondition(lsSQL, "a.sStockIDx = " + SQLUtil.toSQL(fsValue));
            System.out.println(lsSQL);
            loRS = poGRider.executeQuery(lsSQL);

            loJSON = showFXDialog.jsonBrowse(poGRider, loRS, lsHeader, lsColName);
        } else {
            if (!fbSearch) {
                if (!fsValue.isEmpty()) {
                    if (paDetailOthers.get(fnRow).getValue("sBarCodex").toString().equalsIgnoreCase(fsValue)) {
                        return true;
                    }
                }

                loJSON = showFXDialog.jsonSearch(poGRider,
                        lsSQL,
                        fsValue,
                        lsHeader,
                        lsColName,
                        lsColCrit,
                        0);
            } else {
                if (!fsValue.isEmpty()) {
                    if (paDetailOthers.get(fnRow).getValue("sDescript").equals(fsValue)) {
                        return true;
                    }
                }

                loJSON = showFXDialog.jsonSearch(poGRider,
                        lsSQL,
                        fsValue,
                        lsHeader,
                        lsColName,
                        lsColCrit,
                        1);
            }

        }
        System.err.println(lsSQL);

        if (loJSON != null) {
            setDetail(fnRow, fnCol, (String) loJSON.get("sStockIDx"));
            setDetail(fnRow, "nInvCostx", Double.valueOf((String) loJSON.get("nUnitPrce")));
System.out.println(loJSON.toJSONString());
            paDetailOthers.get(fnRow).setValue("sStockIDx", (String) loJSON.get("sStockIDx"));
            paDetailOthers.get(fnRow).setValue("sBarCodex", (String) loJSON.get("sBarCodex"));
            paDetailOthers.get(fnRow).setValue("sDescript", (String) loJSON.get("sDescript"));
            paDetailOthers.get(fnRow).setValue("nQtyOnHnd", Double.valueOf((String) loJSON.get("nQtyOnHnd")));
            paDetailOthers.get(fnRow).setValue("nResvOrdr", Double.valueOf((String) loJSON.get("nResvOrdr")));
            paDetailOthers.get(fnRow).setValue("nBackOrdr", Double.valueOf((String) loJSON.get("nBackOrdr")));
            paDetailOthers.get(fnRow).setValue("nFloatQty", Double.valueOf((String) loJSON.get("nFloatQty")));
            paDetailOthers.get(fnRow).setValue("nLedgerNo", Integer.valueOf((String) loJSON.get("nLedgerNo")));
            paDetailOthers.get(fnRow).setValue("sInvTypNm", (String) loJSON.get("xInvTypNm"));
            paDetailOthers.get(fnRow).setValue("sMeasurNm", (String) loJSON.get("sMeasurNm"));
            paDetailOthers.get(fnRow).setValue("sBrandNme", (String) loJSON.get("xBrandNme"));

       
            return true;
        } else {
            setDetail(fnRow, fnCol, "");
            setDetail(fnRow, "nInvCostx", 0.00);
            setDetail(fnRow, "nQuantity", 0);

            paDetailOthers.get(fnRow).setValue("sStockIDx", "");
            paDetailOthers.get(fnRow).setValue("sBarCodex", "");
            paDetailOthers.get(fnRow).setValue("sDescript", "");
            paDetailOthers.get(fnRow).setValue("sStockIDx", "");
            paDetailOthers.get(fnRow).setValue("sParentID", "");
            paDetailOthers.get(fnRow).setValue("nQtyOnHnd", 0);
            paDetailOthers.get(fnRow).setValue("nResvOrdr", 0);
            paDetailOthers.get(fnRow).setValue("nBackOrdr", 0);
            paDetailOthers.get(fnRow).setValue("nFloatQty", 0);
            paDetailOthers.get(fnRow).setValue("nLedgerNo", 0);
            paDetailOthers.get(fnRow).setValue("xQuantity", 0);
            paDetailOthers.get(fnRow).setValue("sMeasurNm", "");
            paDetailOthers.get(fnRow).setValue("sBrandNme", "");
            return false;
        }
    }
    public Object getDetailOthers(int fnRow, String fsCol) {
        switch (fsCol) {
            case "sStockIDx":
            case "nQtyOnHnd":
            case "xQtyOnHnd":
            case "nResvOrdr":
            case "nBackOrdr":
            case "nReorderx":
            case "nLedgerNo":
            case "sBarCodex":
            case "sDescript":
            case "sOrigCode":
            case "sOrigDesc":
            case "sOrderNox":
            case "sMeasurNm":
            case "sBrandNme":
                return paDetailOthers.get(fnRow).getValue(fsCol);
            default:
                return null;
        }
    }

    public Object getDetail(int fnRow, int fnCol) {
        if (pnEditMode == EditMode.UNKNOWN) {
            return null;
        } else {
            return paDetail.get(fnRow).getValue(fnCol);
        }
    }

    public Object getDetail(int fnRow, String fsCol) {
        return getDetail(fnRow, poDetail.getColumn(fsCol));
    }


    public boolean newTransaction() {
        Connection loConn = null;
        loConn = setConnection();

        poData = new UnitInvTransferDiscrepancyMaster();
        poData.setTransNox(MiscUtil.getNextCode(poData.getTable(), "sTransNox", true, loConn, psBranchCd));
        poData.setTransact(poGRider.getServerDate());
        poData.setDestinat("PHO1");
        paDetail = new ArrayList<>();
        paDetailOthers = new ArrayList<>(); //detail other info storage
//        addDetail();

        pnEditMode = EditMode.ADDNEW;
        return true;
    }

    private double computeTotal() {
        double lnTranTotal = 0;
        for (int lnCtr = 0; lnCtr <= ItemCount() - 1; lnCtr++) {
            lnTranTotal += (Double.valueOf(getDetail(lnCtr, "nQuantity").toString()) * Double.valueOf(getDetail(lnCtr, "nInvCostx").toString()));
        }

        return lnTranTotal;
    }

    private boolean isInventoryOK(String fsValue) {
        int lnMasRow = poData.getEntryNox();

        String lsSQL = MiscUtil.addCondition(getSQ_Detail(), "sTransNox = " + SQLUtil.toSQL(fsValue));

        try {
            ResultSet loRS = poGRider.executeQuery(lsSQL);

            if (MiscUtil.RecordCount(loRS) != lnMasRow) {
                lsSQL = MiscUtil.makeSelect(new UnitInvTransferDiscrepancyDetail());
                lsSQL = MiscUtil.addCondition(lsSQL, "sTransNox = " + SQLUtil.toSQL(fsValue));

                loRS = poGRider.executeQuery(lsSQL);

                ResultSet loRSx;
                InvMaster loInvMaster = new InvMaster(poGRider, psBranchCd, false);

                while (loRS.next()) {
                    lsSQL = MiscUtil.makeSelect(new UnitInvMaster());
                    lsSQL = MiscUtil.addCondition(lsSQL, "sStockIDx = " + SQLUtil.toSQL(loRS.getString("sStockIDx"))
                            + " AND sBranchCD = " + SQLUtil.toSQL(psBranchCd));
                    System.out.println(lsSQL);
                    loRSx = poGRider.executeQuery(lsSQL);
                    if (!loRSx.next()) {
                        if (loInvMaster.SearchInventory(loRS.getString("sStockIDx"), false, true)) {
                            loInvMaster.NewRecord();
                            if (!loInvMaster.SaveRecord()) {
                                System.err.println(loInvMaster.getMessage());
                                return false;
                            }

                            lsSQL = "SELECT cRecdStat FROM Inventory WHERE sStockIDx = " + SQLUtil.toSQL(loRS.getString("sStockIDx"));
                            ResultSet loRSz = poGRider.executeQuery(lsSQL);

                            if (loRSz.next()) {
                                if (loRSz.getString("cRecdStat").equals("0")) {
                                    //reactivate the item
                                    lsSQL = "UPDATE Inventory SET cRecdStat = '1' WHERE sStockIDx = " + SQLUtil.toSQL(loRS.getString("sStockIDx"));

                                    if (poGRider.executeQuery(lsSQL, "Inventory", psBranchCd, "") <= 0) {
                                        System.err.println(poGRider.getErrMsg());
                                        return false;
                                    }
                                }
                            }

                        }
                    }
                }
            }
        } catch (SQLException ex) {
            System.err.println(ex.getMessage());
            return false;
        }

        return true;
    }

    public boolean openTransaction(String fsTransNox) {
        poData = loadTransaction(fsTransNox);

        if (poData != null) {
            paDetail = loadTransactionDetail(fsTransNox);

            if (poData.getEntryNox() != paDetail.size()) {
                setMessage("Transaction discrepancy detected... \n"
                        + "Detail count is not equal to the entry number...");
                return false;
            }
        } else {
            setMessage("Unable to load transaction.");
            return false;
        }

        pnEditMode = EditMode.READY;
        return true;
    }

    public UnitInvTransferDiscrepancyMaster loadTransaction(String fsTransNox) {
        UnitInvTransferDiscrepancyMaster loObject = new UnitInvTransferDiscrepancyMaster();

        Connection loConn = null;
        loConn = setConnection();

        String lsSQL = MiscUtil.addCondition(getSQ_Master(), "sTransNox = " + SQLUtil.toSQL(fsTransNox));
        ResultSet loRS = poGRider.executeQuery(lsSQL);

        try {
            if (!loRS.next()) {
                setMessage("No Record Found");
            } else {
                //load each column to the entity
                for (int lnCol = 1; lnCol <= loRS.getMetaData().getColumnCount(); lnCol++) {
                    loObject.setValue(lnCol, loRS.getObject(lnCol));
                }
            }
        } catch (SQLException ex) {
            setErrMsg(ex.getMessage());
        } finally {
            MiscUtil.close(loRS);
            if (!pbWithParent) {
                MiscUtil.close(loConn);
            }
        }

        return loObject;
    }

    private ArrayList<UnitInvTransferDiscrepancyDetail> loadTransactionDetail(String fsTransNox) {
        UnitInvTransferDiscrepancyDetail loOcc = null;
        UnitInvTransferDetailOthers loOth = null;
        Connection loConn = null;
        loConn = setConnection();

        ArrayList<UnitInvTransferDiscrepancyDetail> loDetail = new ArrayList<>();
        paDetailOthers = new ArrayList<>(); //reset detail others

        //2024.05.15
        //  Check first if the transferred items are in the destination's inventory
        if (!isInventoryOK(fsTransNox)) {
            return null;
        }

        String lsSQL = MiscUtil.addCondition(getSQ_Detail(), "sTransNox = " + SQLUtil.toSQL(fsTransNox));
        try {
            System.out.println(lsSQL);
            ResultSet loRS = poGRider.executeQuery(lsSQL);

            for (int lnCtr = 1; lnCtr <= MiscUtil.RecordCount(loRS); lnCtr++) {
                loRS.absolute(lnCtr);

                //load detail
                loOcc = new UnitInvTransferDiscrepancyDetail();
                loOcc.setValue("sTransNox", loRS.getString("sTransNox"));
                loOcc.setValue("nEntryNox", loRS.getInt("nEntryNox"));
                loOcc.setValue("sStockIDx", loRS.getString("sStockIDx"));
                loOcc.setValue("nQuantity", loRS.getDouble("nQuantity"));
                loOcc.setValue("nInvCostx", loRS.getDouble("nInvCostx"));
                loOcc.setValue("sRemarksx", loRS.getString("sRemarksx"));
                loOcc.setValue("sNotesxxx", loRS.getString("sNotesxxx"));
                loOcc.setValue("dExpiryDt", loRS.getDate("dExpiryDt"));
                loOcc.setValue("dModified", loRS.getDate("dModified"));
                loDetail.add(loOcc);

                //load other info
                loOth = new UnitInvTransferDetailOthers();
                loOth.setValue("sStockIDx", loRS.getString("sStockIDx"));
                loOth.setValue("sBarCodex", loRS.getString("sBarCodex"));
                loOth.setValue("sDescript", loRS.getString("sDescript"));
                loOth.setValue("nQtyOnHnd", loRS.getDouble("nQtyOnHnd"));
                loOth.setValue("xQtyOnHnd", loRS.getDouble("xQtyOnHnd"));
                loOth.setValue("nResvOrdr", loRS.getDouble("nResvOrdr"));
                loOth.setValue("nBackOrdr", loRS.getDouble("nBackOrdr"));
                loOth.setValue("nReorderx", 0);
                loOth.setValue("nLedgerNo", loRS.getInt("nLedgerNo"));
                loOth.setValue("sOrigCode", loRS.getString("sBarCodex"));
                loOth.setValue("sBrandNme", loRS.getString("xBrandNme"));
//                System.out.println();
                if (loRS.getString("sMeasurNm") != null) {
                    loOth.setValue("sMeasurNm", loRS.getString("sMeasurNm"));
                } else {
                    loOth.setValue("sMeasurNm", "");
                }
                paDetailOthers.add(loOth);
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
            return null;
        }

        return loDetail;
    }

    private boolean saveInvTrans() {
        String lsSQL = "";
        String lsStockNo = "";
        ResultSet loRS = null;
        int lnCtr;
        InventoryTrans loInvTrans = new InventoryTrans(poGRider, poGRider.getBranchCode());

        /*---------------------------------------------------------------------------------
         *   Save inventory trans of the items
         *---------------------------------------------------------------------------------*/
         loInvTrans.InitTransaction();
        for (lnCtr = 0; lnCtr <= paDetail.size() - 1; lnCtr++) {
            
            if (paDetail.get(lnCtr).getStockIDx().equals("")) {
                break;
            }

            lsSQL = "SELECT"
                    + "  nQtyOnHnd"
                    + ", nResvOrdr"
                    + ", nBackOrdr"
                    + ", nLedgerNo"
                    + " FROM Inv_Master"
                    + " WHERE sStockIDx = " + SQLUtil.toSQL(paDetail.get(lnCtr).getStockIDx())
                    + " AND sBranchCd = " + SQLUtil.toSQL(psBranchCd);

            loRS = poGRider.executeQuery(lsSQL);

            loInvTrans.setDetail(lnCtr, "sStockIDx", paDetail.get(lnCtr).getStockIDx());
            loInvTrans.setDetail(lnCtr, "nQuantity", paDetail.get(lnCtr).getQuantity());

            if (MiscUtil.RecordCount(loRS) == 0) {
                loInvTrans.setDetail(lnCtr, "nQtyOnHnd", 0);
                loInvTrans.setDetail(lnCtr, "nResvOrdr", 0);
                loInvTrans.setDetail(lnCtr, "nBackOrdr", 0);
            } else {
                try {
                    loRS.first();
                    loInvTrans.setDetail(lnCtr, "nQtyOnHnd", loRS.getDouble("nQtyOnHnd"));
                    loInvTrans.setDetail(lnCtr, "nResvOrdr", loRS.getDouble("nResvOrdr"));
                    loInvTrans.setDetail(lnCtr, "nBackOrdr", loRS.getDouble("nBackOrdr"));
                    loInvTrans.setDetail(lnCtr, "nLedgerNo", loRS.getInt("nLedgerNo"));
                } catch (SQLException e) {
                    setMessage("Please inform MIS Department.");
                    setErrMsg(e.getMessage());
                    return false;
                }
            }
        }

        if (!loInvTrans.DeliveryDiscrepancy(poData.getTransNox(), poData.getTransact(), EditMode.ADDNEW)) {
            setMessage(loInvTrans.getMessage());
            setErrMsg(loInvTrans.getErrMsg());
            return false;
        }

        return true;
    }


    private boolean postInvTrans(Date fdReceived) {
        String lsSQL = "";
        ResultSet loRS = null;
        int lnCtr;

        InventoryTrans loInvTrans = new InventoryTrans(poGRider, poGRider.getBranchCode());
        /*---------------------------------------------------------------------------------
         *   Save inventory trans of the items
         *---------------------------------------------------------------------------------*/
        loInvTrans.InitTransaction();
        for (lnCtr = 0; lnCtr <= paDetail.size() - 1; lnCtr++) {
            if (paDetail.get(lnCtr).getStockIDx().equals("")) {
                break;
            }

            lsSQL = "SELECT"
                    + "  nQtyOnHnd"
                    + ", nResvOrdr"
                    + ", nBackOrdr"
                    + ", nLedgerNo"
                    + " FROM Inv_Master"
                    + " WHERE sStockIDx = " + SQLUtil.toSQL(paDetail.get(lnCtr).getStockIDx())
                    + " AND sBranchCd = " + SQLUtil.toSQL(psBranchCd);

            loRS = poGRider.executeQuery(lsSQL);

            loInvTrans.setDetail(lnCtr, "sStockIDx", paDetail.get(lnCtr).getStockIDx());
            loInvTrans.setDetail(lnCtr, "nQuantity", paDetail.get(lnCtr).getQuantity());

            if (MiscUtil.RecordCount(loRS) == 0) {
                loInvTrans.setDetail(lnCtr, "nQtyOnHnd", 0);
                loInvTrans.setDetail(lnCtr, "nResvOrdr", 0);
                loInvTrans.setDetail(lnCtr, "nBackOrdr", 0);
            } else {
                try {
                    loRS.first();
                    loInvTrans.setDetail(lnCtr, "nQtyOnHnd", loRS.getDouble("nQtyOnHnd"));
                    loInvTrans.setDetail(lnCtr, "nResvOrdr", loRS.getDouble("nResvOrdr"));
                    loInvTrans.setDetail(lnCtr, "nBackOrdr", loRS.getDouble("nBackOrdr"));
                    loInvTrans.setDetail(lnCtr, "nLedgerNo", loRS.getInt("nLedgerNo"));
                } catch (SQLException e) {
                    setMessage("Please inform MIS Department.");
                    setErrMsg(e.getMessage());
                    return false;
                }
            }
        }

        if (!loInvTrans.AcceptDeliveryDiscrepancy(poData.getTransNox(), fdReceived, EditMode.ADDNEW)) {
            setMessage(loInvTrans.getMessage());
            setErrMsg(loInvTrans.getErrMsg());
            return false;
        }
        return true;
    }


    public boolean updateRecord() {
        if (pnEditMode != EditMode.READY) {
            return false;
        } else {
            pnEditMode = EditMode.UPDATE;
            return true;
        }
    }

    public boolean saveTransaction() {
        String lsSQL = "";
        boolean lbUpdate = false;

        UnitInvTransferDiscrepancyMaster loOldEnt = null;
        UnitInvTransferDiscrepancyMaster loNewEnt = null;
        UnitInvTransferDiscrepancyMaster loResult = null;

        // Check for the value of foEntity
        if (!(poData instanceof UnitInvTransferDiscrepancyMaster)) {
            setErrMsg("Invalid Entity Passed as Parameter");
            return false;
        }

        // Typecast the Entity to this object
        loNewEnt = (UnitInvTransferDiscrepancyMaster) poData;

        if (loNewEnt.getDestinat() == null || loNewEnt.getDestinat().equals("")) {
            setMessage("Invalid destination detected.");
            return false;
        }

        if (!pbWithParent) {
            poGRider.beginTrans();
        }

        poData.setTranTotl(computeTotal());

        //delete empty detail
        if (paDetail.get(ItemCount() - 1).getStockIDx().equals("")) {
            deleteDetail(ItemCount() - 1);
        }

        // Generate the SQL Statement
        if (pnEditMode == EditMode.ADDNEW) {
            Connection loConn = null;
            loConn = setConnection();

            String lsTransNox = MiscUtil.getNextCode(loNewEnt.getTable(), "sTransNox", true, loConn, psBranchCd);

            loNewEnt.setTransNox(lsTransNox);
            loNewEnt.setBranchCd(poGRider.getBranchCode());
            loNewEnt.setEntryNox(ItemCount());
            loNewEnt.setModified(psUserIDxx);
            loNewEnt.setDateModified(poGRider.getServerDate());

            if (!pbWithParent) {
                MiscUtil.close(loConn);
            }
            lbUpdate = saveDetail(loNewEnt.getTransNox());
            
//            lbUpdate = saveInvTrans(); //save inventory legder
            if (!lbUpdate) {
                lsSQL = "";
            } else {
                lsSQL = MiscUtil.makeSQL((GEntity) loNewEnt);
            }
        } else {
            //Load previous transaction
            loOldEnt = loadTransaction(poData.getTransNox());

            loNewEnt.setEntryNox(ItemCount());
            loNewEnt.setDateModified(poGRider.getServerDate());

            lbUpdate = saveDetail(loNewEnt.getTransNox());
            if (!lbUpdate) {
                lsSQL = "";
            } else {
                lsSQL = MiscUtil.makeSQL((GEntity) loNewEnt, (GEntity) loOldEnt, "sTransNox = " + SQLUtil.toSQL(loNewEnt.getValue(1)));
            }
        }

        if (!lsSQL.equals("") && getErrMsg().isEmpty()) {
            if (poGRider.executeQuery(lsSQL, loNewEnt.getTable(), "", "") == 0) {
                if (!poGRider.getErrMsg().isEmpty()) {
                    setErrMsg(poGRider.getErrMsg());
                } else {
                    setMessage("No record updated");
                }
            }
             
        }

        if (!pbWithParent) {
            if (!getErrMsg().isEmpty()) {
                poGRider.rollbackTrans();
            } else {
                poGRider.commitTrans();
            }
        }

        return lbUpdate;
    }

    private boolean saveDetail(String fsTransNox) {
        setMessage("");
        int lnCtr;
        String lsSQL;
        UnitInvTransferDiscrepancyDetail loNewEnt = null;

        for (lnCtr = 0; lnCtr <= paDetail.size() - 1; lnCtr++) {
            if (paDetail.isEmpty()) {
                setMessage("Unable to save empty detail transaction.");
                return false;
            } else if (paDetail.get(0).getStockIDx().equals("")
                    || paDetail.get(0).getQuantity().doubleValue() == 0.00) {
                setMessage("Detail might not have item or zero quantity.");
                return false;
            }
        }
        if (pnEditMode == EditMode.ADDNEW) {
            Connection loConn = null;
            loConn = setConnection();

            for (lnCtr = 0; lnCtr <= paDetail.size() - 1; lnCtr++) {
                loNewEnt = paDetail.get(lnCtr);

                if (!loNewEnt.getStockIDx().equals("")) {
                    if (loNewEnt.getQuantity().doubleValue() == 0.00) {
                        setMessage("Detail might not have item or zero quantity.");
                        return false;
                    }

                    loNewEnt.setTransNox(fsTransNox);
                    loNewEnt.setEntryNox(lnCtr + 1);
                    loNewEnt.setDateModified(poGRider.getServerDate());

                    lsSQL = MiscUtil.makeSQL((GEntity) loNewEnt);

                    if (!lsSQL.equals("")) {
                        if (poGRider.executeQuery(lsSQL, loNewEnt.getTable(), "", "") == 0) {
                            if (!poGRider.getErrMsg().isEmpty()) {
                                setErrMsg(poGRider.getErrMsg());
                                return false;
                            }
                        }
                    }
                }
            }
        }

        return true;
    }

    public boolean deleteTransaction(String string) {
        UnitInvTransferDiscrepancyMaster loObject = loadTransaction(string);
        boolean lbResult = false;

        if (loObject == null) {
            setMessage("No record found...");
            return lbResult;
        }

        String lsSQL = "DELETE FROM " + loObject.getTable()
                + " WHERE sTransNox = " + SQLUtil.toSQL(string);

        if (!pbWithParent) {
            poGRider.beginTrans();
        }

        if (poGRider.executeQuery(lsSQL, loObject.getTable(), "", "") == 0) {
            if (!poGRider.getErrMsg().isEmpty()) {
                setErrMsg(poGRider.getErrMsg());
            } else {
                setErrMsg("No record deleted.");
            }
        } else {
            lbResult = true;
        }

        //delete detail rows
        lsSQL = "DELETE FROM " + poDetail.getTable()
                + " WHERE sTransNox = " + SQLUtil.toSQL(string);

        if (poGRider.executeQuery(lsSQL, poDetail.getTable(), "", "") == 0) {
            if (!poGRider.getErrMsg().isEmpty()) {
                setErrMsg(poGRider.getErrMsg());
            } else {
                setErrMsg("No record deleted.");
            }
        } else {
            lbResult = true;
        }

        if (!pbWithParent) {
            if (getErrMsg().isEmpty()) {
                poGRider.commitTrans();
            } else {
                poGRider.rollbackTrans();
            }
        }

        return lbResult;
    }

    public boolean closeTransaction(String string) {
        UnitInvTransferDiscrepancyMaster loObject = loadTransaction(string);
        boolean lbResult = false;

        if (loObject == null) {
            setMessage("No record found...");
            return lbResult;
        }

        //if it is already closed, just return true
        if (loObject.getTranStat().equalsIgnoreCase(TransactionStatus.STATE_CLOSED)) {
            return true;
        }

        if (!loObject.getTranStat().equalsIgnoreCase(TransactionStatus.STATE_OPEN)) {
            setMessage("Unable to close closed/cancelled/posted/voided transaction.");
            return lbResult;
        }

        String lsSQL = "UPDATE " + loObject.getTable()
                + " SET  cTranStat = " + SQLUtil.toSQL(TransactionStatus.STATE_CLOSED)
                + ", sModified = " + SQLUtil.toSQL(psUserIDxx)
                + ", dModified = " + SQLUtil.toSQL(poGRider.getServerDate())
                + " WHERE sTransNox = " + SQLUtil.toSQL(loObject.getTransNox());

        if (!pbWithParent) {
            poGRider.beginTrans();
        }

        if (poGRider.executeQuery(lsSQL, loObject.getTable(), "", "") == 0) {
            if (!poGRider.getErrMsg().isEmpty()) {
                setErrMsg(poGRider.getErrMsg());
            } else {
                setErrMsg("Unable to close transaction.");
            }
        } else {
            lbResult = saveInvTrans();
        }

        if (!pbWithParent) {
            if (getErrMsg().isEmpty()) {
                poGRider.commitTrans();
            } else {
                poGRider.rollbackTrans();
            }
        }

        return lbResult;
    }

    public boolean postTransaction(String string, Date received) {
        UnitInvTransferDiscrepancyMaster loObject = loadTransaction(string);
        boolean lbResult = false;

        if (loObject == null) {
            setMessage("No record found...");
            return lbResult;
        }

        if (loObject.getTranStat().equalsIgnoreCase(TransactionStatus.STATE_POSTED)
                || loObject.getTranStat().equalsIgnoreCase(TransactionStatus.STATE_CANCELLED)
                || loObject.getTranStat().equalsIgnoreCase(TransactionStatus.STATE_VOID)) {
            setMessage("Unable to close proccesed transaction.");
            return lbResult;
        }

        String lsSQL = "UPDATE " + loObject.getTable()
                + " SET  cTranStat = " + SQLUtil.toSQL(TransactionStatus.STATE_POSTED)
                + ", sModified = " + SQLUtil.toSQL(psUserIDxx)
                + ", dModified = " + SQLUtil.toSQL(received)
                + " WHERE sTransNox = " + SQLUtil.toSQL(loObject.getTransNox());

        if (!pbWithParent) {
            poGRider.beginTrans();
        }

        if (poGRider.executeQuery(lsSQL, loObject.getTable(), "", "") == 0) {
            if (!poGRider.getErrMsg().isEmpty()) {
                setErrMsg(poGRider.getErrMsg());
            } else {
                setErrMsg("Tranasction was not posted.");
            }
        } else {
            lbResult = postInvTrans(received);
        }

        if (!pbWithParent) {
            if (getErrMsg().isEmpty()) {
                poGRider.commitTrans();
            } else {
                poGRider.rollbackTrans();
            }
        }
        return lbResult;
    }

    public boolean voidTransaction(String string) {
        UnitInvTransferDiscrepancyMaster loObject = loadTransaction(string);
        boolean lbResult = false;

        if (loObject == null) {
            setMessage("No record found...");
            return lbResult;
        }

        if (loObject.getTranStat().equalsIgnoreCase(TransactionStatus.STATE_POSTED)
                || loObject.getTranStat().equalsIgnoreCase(TransactionStatus.STATE_CANCELLED)
                || loObject.getTranStat().equalsIgnoreCase(TransactionStatus.STATE_VOID)) {
            setMessage("Unable to close processed transaction.");
            return lbResult;
        }

        String lsSQL = "UPDATE " + loObject.getTable()
                + " SET  cTranStat = " + SQLUtil.toSQL(TransactionStatus.STATE_VOID)
                + ", sModified = " + SQLUtil.toSQL(psUserIDxx)
                + ", dModified = " + SQLUtil.toSQL(poGRider.getServerDate())
                + " WHERE sTransNox = " + SQLUtil.toSQL(loObject.getTransNox());

        if (!pbWithParent) {
            poGRider.beginTrans();
        }

        if (poGRider.executeQuery(lsSQL, loObject.getTable(), "", "") == 0) {
            if (!poGRider.getErrMsg().isEmpty()) {
                setErrMsg(poGRider.getErrMsg());
            } else {
                setErrMsg("No record deleted.");
            }
        } else {
            lbResult = true;
        }

        if (!pbWithParent) {
            if (getErrMsg().isEmpty()) {
                poGRider.commitTrans();
            } else {
                poGRider.rollbackTrans();
            }
        }
        return lbResult;
    }

    public boolean cancelTransaction(String string) {
        UnitInvTransferDiscrepancyMaster loObject = loadTransaction(string);
        boolean lbResult = false;

        if (loObject == null) {
            setMessage("No record found...");
            return lbResult;
        }

        if (loObject.getTranStat().equalsIgnoreCase(TransactionStatus.STATE_POSTED)
                || loObject.getTranStat().equalsIgnoreCase(TransactionStatus.STATE_CANCELLED)
                || loObject.getTranStat().equalsIgnoreCase(TransactionStatus.STATE_VOID)) {
            setMessage("Unable to close processed transaction.");
            return lbResult;
        }

        String lsSQL = "UPDATE " + loObject.getTable()
                + " SET  cTranStat = " + SQLUtil.toSQL(TransactionStatus.STATE_CANCELLED)
                + ", sModified = " + SQLUtil.toSQL(psUserIDxx)
                + ", dModified = " + SQLUtil.toSQL(poGRider.getServerDate())
                + " WHERE sTransNox = " + SQLUtil.toSQL(loObject.getTransNox());

        if (!pbWithParent) {
            poGRider.beginTrans();
        }

        if (poGRider.executeQuery(lsSQL, loObject.getTable(), "", "") == 0) {
            if (!poGRider.getErrMsg().isEmpty()) {
                setErrMsg(poGRider.getErrMsg());
            } else {
                setErrMsg("No record deleted.");
            }
        } else {
            lbResult = true;
        }

        if (!pbWithParent) {
            if (getErrMsg().isEmpty()) {
                poGRider.commitTrans();
            } else {
                poGRider.rollbackTrans();
            }
        }
        return lbResult;
    }


    public void setMaster(int fnCol, Object foData) {
        if (pnEditMode != EditMode.UNKNOWN) {
            // Don't allow specific fields to assign values
            if (!(fnCol == poData.getColumn("sTransNox")
                    || fnCol == poData.getColumn("nEntryNox")
                    || fnCol == poData.getColumn("cTranStat")
                    || fnCol == poData.getColumn("sModified")
                    || fnCol == poData.getColumn("dModified"))) {

                if (fnCol == poData.getColumn("nFreightx")
                        || fnCol == poData.getColumn("nTranTotl")
                        || fnCol == poData.getColumn("nDiscount")) {
                    if (foData instanceof Number) {
                        poData.setValue(fnCol, foData);
                    } else {
                        poData.setValue(fnCol, 0.00);
                    }
                } else {
                    poData.setValue(fnCol, foData);
                }

                MasterRetreived(fnCol);
            }
        }
    }

    public void setMaster(String fsCol, Object foData) {
        setMaster(poData.getColumn(fsCol), foData);
    }

    public Object getMaster(int fnCol) {
        if (pnEditMode == EditMode.UNKNOWN) {
            return null;
        } else {
            return poData.getValue(fnCol);
        }
    }

    public Object getMaster(String fsCol) {
        return getMaster(poData.getColumn(fsCol));
    }

    public String getMessage() {
        return psWarnMsg;
    }

    public void setMessage(String string) {
        psWarnMsg = string;
    }

    public String getErrMsg() {

        return psErrMsgx;
    }

    public void setErrMsg(String string) {
        psErrMsgx = string;
    }

    public void setBranch(String string) {
        psBranchCd = string;
    }

    public void setWithParent(boolean bln) {
        pbWithParent = bln;
    }

    public String getSQ_Master() {
        return MiscUtil.makeSelect(new UnitInvTransferDiscrepancyMaster());
    }

    private String getSQ_Detail() {
        return "SELECT"
                + "  a.sTransNox"
                + ", a.nEntryNox"
                + ", a.sStockIDx"
                + ", a.nQuantity"
                + ", a.nInvCostx"
                + ", a.sRemarksx"
                + ", a.sNotesxxx"
                + ", a.dExpiryDt"
                + ", a.dModified"
                + ", b.nQtyOnHnd"
                + ", b.nQtyOnHnd + a.nQuantity xQtyOnHnd"
                + ", b.nResvOrdr"
                + ", b.nBackOrdr"
                + ", b.nFloatQty"
                + ", b.nLedgerNo"
                + ", c.sBarCodex"
                + ", c.sDescript"
                + ", e.sMeasurNm"
                + ", f.sDescript xBrandNme"
                + " FROM Inv_Transfer_Discrepancy_Detail a"
                + ", Inv_Master b"
                + " LEFT JOIN Inventory c"
                + " ON b.sStockIDx = c.sStockIDx"
                + " LEFT JOIN Brand f"
                + " ON c.sBrandCde = f.sBrandCde"
                + " LEFT JOIN Measure e"
                + " ON c.sMeasurID = e.sMeasurID"
                + " WHERE a.sStockIDx = b.sStockIDx"
                + " AND b.sBranchCD = " + SQLUtil.toSQL(psBranchCd)
                + " ORDER BY a.nEntryNox";
    }

    public int ItemCount() {
        return paDetail.size();
    }

    public Inventory GetInventory(String fsValue, boolean fbByCode, boolean fbSearch) {
        Inventory instance = new Inventory(poGRider, psBranchCd, fbSearch);
        instance.BrowseRecord(fsValue, fbByCode, false);
        return instance;
    }

    public XMBranch GetBranch(String fsValue, boolean fbByCode) {
        if (fbByCode && fsValue.equals("")) {
            return null;
        }

        XMBranch instance = new XMBranch(poGRider, psBranchCd, true);
        if (instance.browseRecord(fsValue, fbByCode)) {
            return instance;
        } else {
            return null;
        }
    }

    private Connection setConnection() {
        Connection foConn;

        if (pbWithParent) {
            foConn = (Connection) poGRider.getConnection();
            if (foConn == null) {
                foConn = (Connection) poGRider.doConnect();
            }
        } else {
            foConn = (Connection) poGRider.doConnect();
        }

        return foConn;
    }

    public int getEditMode() {
        return pnEditMode;
    }

    private String getSQ_InvTransferDiscrepancy() {
        String lsTranStat = String.valueOf(pnTranStat);
        String lsCondition = "";
        String lsSQL = "SELECT "
                + "  a.sTransNox"
                + ", b.sBranchNm"
                + ", DATE_FORMAT(a.dTransact, '%m/%d/%Y') AS dTransact"
                + ", c.sBranchNm"
                + " FROM Inv_Transfer_Discrepancy_Master a"
                + " LEFT JOIN Branch b"
                + " ON a.sDestinat = b.sBranchCd"
                + " LEFT JOIN Branch c"
                + " ON LEFT(a.sTransNox, 4) = c.sBranchCd";
        //" WHERE a.sTransNox LIKE " + SQLUtil.toSQL(psBranchCd + "%");

        if (lsTranStat.length() == 1) {
            lsCondition = "a.cTranStat = " + SQLUtil.toSQL(lsTranStat);
        } else {
            for (int lnCtr = 0; lnCtr <= lsTranStat.length() - 1; lnCtr++) {
                lsCondition = lsCondition + SQLUtil.toSQL(String.valueOf(lsTranStat.charAt(lnCtr))) + ",";
            }
            lsCondition = "(" + lsCondition.substring(0, lsCondition.length() - 1) + ")";
            lsCondition = "a.cTranStat IN " + lsCondition;
        }

        lsSQL = MiscUtil.addCondition(lsSQL, lsCondition);
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
                + ", b.sDescript xBrandNme"
                + ", c.sDescript xModelNme"
                + ", d.sDescript xInvTypNm"
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
                + " AND e.sBranchCd = " + SQLUtil.toSQL(psBranchCd);



        return lsSQL;
    }

    public void printColumnsMaster() {
        poData.list();
    }

    public void printColumnsDetail() {
        poDetail.list();
    }

    public void setTranStat(int fnValue) {
        this.pnTranStat = fnValue;
    }

    //callback methods
    public void setCallBack(IMasterDetail foCallBack) {
        poCallBack = foCallBack;
    }

    private void MasterRetreived(int fnRow) {
        if (poCallBack == null) {
            return;
        }

        poCallBack.MasterRetreive(fnRow);
    }

    private void DetailRetreived(int fnRow) {
        if (poCallBack == null) {
            return;
        }

        poCallBack.DetailRetreive(fnRow);
    }

    //Member Variables
    private GRider poGRider = null;
    private String psUserIDxx = "";
    private String psBranchCd = "";
    private String psWarnMsg = "";
    private String psErrMsgx = "";
    private boolean pbWithParent = false;
    private int pnEditMode;
    private int pnTranStat = 0;
    private IMasterDetail poCallBack;

    private UnitInvTransferDiscrepancyMaster poData = new UnitInvTransferDiscrepancyMaster();
    private UnitInvTransferDiscrepancyDetail poDetail = new UnitInvTransferDiscrepancyDetail();
    private ArrayList<UnitInvTransferDiscrepancyDetail> paDetail;
    private ArrayList<UnitInvTransferDetailOthers> paDetailOthers;;


    private final String pxeModuleName = "InvTransferDiscrepancy";
    private double xOffset = 0;
    private double yOffset = 0;
}
