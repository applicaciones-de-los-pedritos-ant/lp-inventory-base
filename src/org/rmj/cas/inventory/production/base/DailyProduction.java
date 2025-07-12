/**
 * Inventory Transfer BASE
 *
 * @author Michael Torres Cuison
 * @since 2018.10.10
 */
package org.rmj.cas.inventory.production.base;

import com.mysql.jdbc.Connection;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.data.JsonDataSource;
import net.sf.jasperreports.view.JasperViewer;
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
import org.rmj.cas.inventory.base.InventoryTrans;
import org.rmj.cas.inventory.others.pojo.UnitDailyProductionDetailOthers;
import org.rmj.cas.inventory.production.pojo.UnitDailyProductionDetail;
import org.rmj.cas.inventory.production.pojo.UnitDailyProductionMaster;
import org.rmj.appdriver.agentfx.callback.IMasterDetail;
import org.rmj.appdriver.constants.UserRight;
import org.rmj.cas.inventory.base.InvExpiration;
import org.rmj.cas.inventory.base.InvRequest;
import org.rmj.cas.inventory.base.InvTransfer;
import org.rmj.cas.inventory.others.pojo.UnitDailyProductionInvOthers;
import org.rmj.cas.inventory.production.pojo.UnitDailyProductionInv;
import org.rmj.lp.parameter.agent.XMBranch;

public class DailyProduction {

    public DailyProduction(GRider foGRider, String fsBranchCD, boolean fbWithParent) {
        this.poGRider = foGRider;

        if (foGRider != null) {
            this.pbWithParent = fbWithParent;
            this.psBranchCd = fsBranchCD;

            this.psUserIDxx = foGRider.getUserID();
            pnEditMode = EditMode.UNKNOWN;
        }
    }

    public boolean BrowseRecord(String fsValue, boolean fbByCode) {
        String lsHeader = "Trans. No»Date»Remarks";
        String lsColName = "a.sTransNox»dTransact»a.sRemarksx";
        String lsColCrit = "a.sTransNox»dTransact»a.sRemarksx";
        String lsSQL = getSQ_DailyProducton();
        JSONObject loJSON;

        loJSON = showFXDialog.jsonSearch(poGRider,
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

    public boolean BrowseProductRequest() {

        try {
            ProductionRequest loProdRequest = new ProductionRequest(poGRider, poGRider.getBranchCode(), true);
            loProdRequest.setTranStat(1);//fetch only printed / confirmed

            if (!loProdRequest.SearchRecord("", true)) {
                return false;
            }
            int lnCtr = loProdRequest.getItemCount();
            setMaster("sSourceCd", "");
            setMaster("sSourceNo", "");
            if (lnCtr < 0) {
                return false;
            }

            //copy the data to daily 
            for (int lnRow = 0; lnRow <= lnCtr - 1; lnRow++) {
                addDetail();
                System.out.println(ItemCount());

//                setDetail(lnRow, "nQuantity", loProdRequest.getDetail(lnRow, "nQuantity"));
                setDetail(lnRow, "sStockIDx", loProdRequest.getDetail(lnRow + 1, "sStockIDx"));
                setDetail(lnRow, "nGoalQtyx", loProdRequest.getDetail(lnRow + 1, "nQuantity"));
                setDetail(lnRow, "nOrderQty", loProdRequest.getDetail(lnRow + 1, "nQuantity"));

                paDetailOthers.get(lnRow).setValue("sBarCodex", loProdRequest.getDetail(lnRow + 1, "sBarCodex"));
                paDetailOthers.get(lnRow).setValue("sDescript", loProdRequest.getDetail(lnRow + 1, "sDescript"));
                paDetailOthers.get(lnRow).setValue("sMeasurNm", loProdRequest.getDetail(lnRow + 1, "sMeasurNm"));
//                addDetail();

            }

//            deleteDetail(ItemCount() - 1);
            setMaster("sSourceCd", "PReq");
            setMaster("sSourceNo", loProdRequest.getMaster("sTransNox"));

        } catch (SQLException ex) {
            Logger.getLogger(DailyProduction.class.getName()).log(Level.SEVERE, null, ex);
        }
        return true;
    }

    public boolean addDetail() {
        if (!String.valueOf(getMaster("sSourceNo")).isEmpty()) {
            return false;
        }
        if (paDetail.isEmpty()) {
            paDetail.add(new UnitDailyProductionDetail());
            paDetail.get(ItemCount() - 1).setDateExpiryDt(poGRider.getServerDate());

            paDetailOthers.add(new UnitDailyProductionDetailOthers());
        } else {
            System.out.println(paDetail.get(ItemCount() - 1).getStockIDx());
            System.out.println(paDetail.get(ItemCount() - 1).getGoalQty());
            if (!paDetail.get(ItemCount() - 1).getStockIDx().equals("")
                    && Double.valueOf(String.valueOf(paDetail.get(ItemCount() - 1).getQuantity())) > 0) {

                paDetail.add(new UnitDailyProductionDetail());
                paDetail.get(ItemCount() - 1).setDateExpiryDt(poGRider.getServerDate());

                paDetailOthers.add(new UnitDailyProductionDetailOthers());
            }
//            if (!paDetail.get(ItemCount()-1).getStockIDx().equals("") &&
//                    Double.valueOf(String.valueOf(paDetail.get(ItemCount() -1).getQuantity()))!= 0.00){
//                paDetail.add(new UnitDailyProductionDetail());
//                paDetail.get(ItemCount() - 1).setDateExpiryDt(poGRider.getServerDate());
//                
//                paDetailOthers.add(new UnitDailyProductionDetailOthers());
//            }

        }
        return true;
    }

    public boolean deleteDetail(int fnRow) {
        paDetail.remove(fnRow);
        paDetailOthers.remove(fnRow);

        if (paDetail.isEmpty()) {
            paDetail.add(new UnitDailyProductionDetail());
            paDetailOthers.add(new UnitDailyProductionDetailOthers());
        }

//        paDetail.get(ItemCount() - 1).setDateExpiryDt(poGRider.getServerDate());
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
                        addDetail();
                    } else {
                        paDetail.get(fnRow).setValue(fnCol, 0);
                    }
                } else if (fnCol == poDetail.getColumn("nGoalQtyx")) {
                    if (foData instanceof Number) {
                        paDetail.get(fnRow).setValue(fnCol, foData);
                    } else {
                        paDetail.get(fnRow).setValue(fnCol, null);
                    }
                } else if (fnCol == poDetail.getColumn("nOrderQty")) {
                    if (foData instanceof Number) {
                        paDetail.get(fnRow).setValue(fnCol, foData);
                    } else {
                        paDetail.get(fnRow).setValue(fnCol, null);
                    }
                } else if (fnCol == poDetail.getColumn("dExpiryDt")) {
                    if (foData instanceof Date) {
                        paDetail.get(fnRow).setValue(fnCol, foData);
                    } else {
                        paDetail.get(fnRow).setValue(fnCol, null);
                    }
                } else {
                    paDetail.get(fnRow).setValue(fnCol, foData);
                }

                DetailRetreived(fnCol);
//                addDetail();
            }
        }
    }

    public void setDetail(int fnRow, String fsCol, Object foData) {
        setDetail(fnRow, poDetail.getColumn(fsCol), foData);
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

    public Object getDetailOthers(int fnRow, String fsCol) {
        switch (fsCol) {
            case "sStockIDx":
            case "sBarCodex":
            case "sDescript":
            case "sMeasurNm":
                return paDetailOthers.get(fnRow).getValue(fsCol);
            default:
                return null;
        }
    }

    public boolean addInv() {
        if (paInv.isEmpty()) {
            paInv.add(new UnitDailyProductionInv());
            paInv.get(InvCount() - 1).setDateExpiryDt(poGRider.getServerDate());

            paInvOthers.add(new UnitDailyProductionInvOthers());
        } else {
            if (!paInv.get(InvCount() - 1).getStockIDx().equals("")
                    && paInv.get(InvCount() - 1).getQtyReqrd() != (Number) 0
                    && paInv.get(InvCount() - 1).getQtyUsed() != (Number) 0) {
                paInv.add(new UnitDailyProductionInv());
                paInv.get(InvCount() - 1).setDateExpiryDt(poGRider.getServerDate());

                paInvOthers.add(new UnitDailyProductionInvOthers());
            }
        }
        return true;
    }

    public boolean deleteInv(int fnRow) {
        paInv.remove(fnRow);
        paInvOthers.remove(fnRow);

        if (paInv.isEmpty()) {
            paInv.add(new UnitDailyProductionInv());
            paInvOthers.add(new UnitDailyProductionInvOthers());
        }

//        paDetail.get(ItemCount() - 1).setDateExpiryDt(poGRider.getServerDate());
        return true;
    }

    public void setInv(int fnRow, int fnCol, Object foData) {
        if (pnEditMode != EditMode.UNKNOWN) {
            // Don't allow specific fields to assign values
            if (!(fnCol == poInv.getColumn("sTransNox")
                    || fnCol == poInv.getColumn("nEntryNox")
                    || fnCol == poInv.getColumn("dModified"))) {

                if (fnCol == poInv.getColumn("nQtyReqrd")) {
                    if (foData instanceof Number) {
                        paInv.get(fnRow).setValue(fnCol, foData);
                    } else {
                        paInv.get(fnRow).setValue(fnCol, 0);
                    }
                } else if (fnCol == poInv.getColumn("nQtyUsedx")) {
                    if (foData instanceof Number) {
                        paInv.get(fnRow).setValue(fnCol, foData);
                        addInv();
                    } else {
                        paInv.get(fnRow).setValue(fnCol, 0);
                    }
                } else if (fnCol == poInv.getColumn("dExpiryDt")) {
                    if (foData instanceof Date) {
                        paInv.get(fnRow).setValue(fnCol, foData);
                    } else {
                        paInv.get(fnRow).setValue(fnCol, poGRider.getServerDate());
                    }
                } else {
                    paInv.get(fnRow).setValue(fnCol, foData);
                }

                DetailRetreived(fnCol);
            }
        }
    }

    public void setInv(int fnRow, String fsCol, Object foData) {
        setInv(fnRow, poInv.getColumn(fsCol), foData);
    }

    public Object getInv(int fnRow, int fnCol) {
        if (pnEditMode == EditMode.UNKNOWN) {
            return null;
        } else {
            return paInv.get(fnRow).getValue(fnCol);
        }
    }

    public Object getInv(int fnRow, String fsCol) {
        return getInv(fnRow, poInv.getColumn(fsCol));
    }

    public Object getInvOthers(int fnRow, String fsCol) {
        switch (fsCol) {
            case "sStockIDx":
            case "sBarCodex":
            case "sDescript":
                return paInvOthers.get(fnRow).getValue(fsCol);
            case "sMeasurNm":
                return paInvOthers.get(fnRow).getValue(fsCol);
            case "sBrandNme":
                return paInvOthers.get(fnRow).getValue(fsCol);
            default:
                return null;
        }
    }

    public boolean newTransaction() {
        Connection loConn = null;
        loConn = setConnection();

        poData = new UnitDailyProductionMaster();
        poData.setTransNox(MiscUtil.getNextCode(poData.getTable(), "sTransNox", true, loConn, psBranchCd));
        poData.setDateTransact(poGRider.getServerDate());

        //init detail
        paDetail = new ArrayList<>();
        paDetailOthers = new ArrayList<>(); //detail other info storage

        paInv = new ArrayList<>();
        paInvOthers = new ArrayList<>(); //detail other info storage

        pnEditMode = EditMode.ADDNEW;

        addDetail();
        addInv();
        return true;
    }

    public boolean openTransaction(String fsTransNox) {
        poData = loadTransaction(fsTransNox);

        if (poData != null) {
            paDetail = loadTransactionDetail(fsTransNox);
            paInv = loadTransactionInv(fsTransNox);
        } else {
            setMessage("Unable to load transaction.");
            return false;
        }

        pnEditMode = EditMode.READY;
        return true;
    }

    public UnitDailyProductionMaster loadTransaction(String fsTransNox) {
        UnitDailyProductionMaster loObject = new UnitDailyProductionMaster();

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

    private ArrayList<UnitDailyProductionDetail> loadTransactionDetail(String fsTransNox) {
        UnitDailyProductionDetail loOcc = null;
        UnitDailyProductionDetailOthers loOth = null;
        Connection loConn = null;
        loConn = setConnection();

        ArrayList<UnitDailyProductionDetail> loDetail = new ArrayList<>();
        paDetailOthers = new ArrayList<>(); //reset detail others
        String lsSQL;

        lsSQL = MiscUtil.addCondition(getSQ_Detail(), "sTransNox = " + SQLUtil.toSQL(fsTransNox));
        System.out.println(lsSQL);
        ResultSet loRS = poGRider.executeQuery(lsSQL);
        try {
            for (int lnCtr = 1; lnCtr <= MiscUtil.RecordCount(loRS); lnCtr++) {
                loRS.absolute(lnCtr);

                loOcc = new UnitDailyProductionDetail();
                loOcc.setValue("sTransNox", loRS.getString("sTransNox"));
                loOcc.setValue("nEntryNox", loRS.getInt("nEntryNox"));
                loOcc.setValue("sStockIDx", loRS.getString("sStockIDx"));
                loOcc.setValue("nGoalQtyx", loRS.getDouble("nGoalQtyx"));
                loOcc.setValue("nOrderQty", loRS.getDouble("nOrderQty"));
                loOcc.setValue("nQuantity", loRS.getDouble("nQuantity"));
                loOcc.setValue("dExpiryDt", loRS.getDate("dExpiryDt"));
                loOcc.setValue("dModified", loRS.getDate("dModified"));
                loDetail.add(loOcc);

                loOth = new UnitDailyProductionDetailOthers();
                loOth.setValue("sStockIDx", loRS.getString("sStockIDx"));
                loOth.setValue("sBrandNme", loRS.getString("xBrandNme"));
                loOth.setValue("sBarCodex", loRS.getString("sBarCodex"));
                loOth.setValue("sDescript", loRS.getString("sDescript"));
                if (loRS.getString("sMeasurNm") != null) {
                    loOth.setValue("sMeasurNm", loRS.getString("sMeasurNm"));
                } else {
                    loOth.setValue("sMeasurNm", "");
                }
                paDetailOthers.add(loOth);
            }
        } catch (SQLException e) {
            //log error message
            return null;
        }

        return loDetail;
    }

    private ArrayList<UnitDailyProductionInv> loadTransactionInv(String fsTransNox) {
        UnitDailyProductionInv loOcc = null;
        UnitDailyProductionInvOthers loOth = null;
        Connection loConn = null;
        loConn = setConnection();

        ArrayList<UnitDailyProductionInv> loDetail = new ArrayList<>();
        paInvOthers = new ArrayList<>(); //reset detail others

        ResultSet loRS = poGRider.executeQuery(
                MiscUtil.addCondition(getSQ_Inv(),
                        "sTransNox = " + SQLUtil.toSQL(fsTransNox)));
        System.out.println(MiscUtil.addCondition(getSQ_Inv(),
                "sTransNox = " + SQLUtil.toSQL(fsTransNox)));
        try {
            for (int lnCtr = 1; lnCtr <= MiscUtil.RecordCount(loRS); lnCtr++) {
                loRS.absolute(lnCtr);

                loOcc = new UnitDailyProductionInv();
                loOcc.setValue("sTransNox", loRS.getString("sTransNox"));
                loOcc.setValue("nEntryNox", loRS.getInt("nEntryNox"));
                loOcc.setValue("sStockIDx", loRS.getString("sStockIDx"));
                loOcc.setValue("nQtyReqrd", loRS.getDouble("nQtyReqrd"));
                loOcc.setValue("nQtyUsedx", loRS.getDouble("nQtyUsedx"));
                loOcc.setValue("dExpiryDt", loRS.getDate("dExpiryDt"));
                loOcc.setValue("dModified", loRS.getDate("dModified"));
                loDetail.add(loOcc);

                loOth = new UnitDailyProductionInvOthers();
                loOth.setValue("sStockIDx", loRS.getString("sStockIDx"));
                loOth.setValue("sBarCodex", loRS.getString("sBarCodex"));
                loOth.setValue("sDescript", loRS.getString("sDescript"));
                loOth.setValue("sMeasurNm", loRS.getString("sMeasurNm"));
                loOth.setValue("sBrandNme", loRS.getString("xBrandNme"));
                paInvOthers.add(loOth);
            }
        } catch (SQLException e) {
            //log error message
            return null;
        }

        return loDetail;
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
        int lnCtr;
        boolean lbUpdate = false;

        UnitDailyProductionMaster loOldEnt = null;
        UnitDailyProductionMaster loNewEnt = null;
        UnitDailyProductionMaster loResult = null;

        // Check for the value of foEntity
        if (!(poData instanceof UnitDailyProductionMaster)) {
            setErrMsg("Invalid Entity Passed as Parameter");
            return false;
        }

        // Typecast the Entity to this object
        loNewEnt = (UnitDailyProductionMaster) poData;

        if (!pbWithParent) {
            poGRider.beginTrans();
        }

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
            loNewEnt.setEntryNox(ItemCount());
            loNewEnt.setModified(psUserIDxx);
            loNewEnt.setDateModified(poGRider.getServerDate());

            if (!pbWithParent) {
                MiscUtil.close(loConn);
            }

            lbUpdate = saveDetail(loNewEnt.getTransNox());
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
            lbUpdate = true;
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
        if (paDetail.isEmpty()) {
            setMessage("Unable to save empty detail transaction.");
            return false;
        }

        int lnCtr;
        String lsSQL;
        UnitDailyProductionDetail loNewEnt = null;

        if (pnEditMode == EditMode.ADDNEW) {
            Connection loConn = null;
            loConn = setConnection();

            for (lnCtr = 0; lnCtr <= paDetail.size() - 1; lnCtr++) {
                loNewEnt = paDetail.get(lnCtr);

                if (!loNewEnt.getStockIDx().equals("")) {
                    loNewEnt.setTransNox(fsTransNox);
                    loNewEnt.setEntryNox(lnCtr + 1);
                    loNewEnt.setDateModified(poGRider.getServerDate());
//                    loNewEnt.setDateExpiryDt(paDetail.get(lnCtr).getDateExpiryDt());

                    lsSQL = MiscUtil.makeSQL((GEntity) loNewEnt, "sBrandNme");

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
        } else {
            ArrayList<UnitDailyProductionDetail> laSubUnit = loadTransactionDetail(poData.getTransNox());

            for (lnCtr = 0; lnCtr <= paDetail.size() - 1; lnCtr++) {
                loNewEnt = paDetail.get(lnCtr);

                if (!loNewEnt.getStockIDx().equals("")) {
                    if (lnCtr <= laSubUnit.size() - 1) {
                        if (loNewEnt.getEntryNox() != lnCtr + 1) {
                            loNewEnt.setEntryNox(lnCtr + 1);
                        }
                        if (loNewEnt.getTransNox().isEmpty()) {
                            loNewEnt.setTransNox(fsTransNox);
                        }
                        System.out.println("sStockIDx = " + loNewEnt.getValue(3));
                        lsSQL = MiscUtil.makeSQL((GEntity) loNewEnt,
                                (GEntity) laSubUnit.get(lnCtr),
                                " sTransNox = " + SQLUtil.toSQL(loNewEnt.getValue(1))
                                + " AND nEntryNox = " + SQLUtil.toSQL(loNewEnt.getValue(2)),
                                "sBrandNme");
                        System.out.println(lsSQL);

                    } else {
                        loNewEnt.setTransNox(fsTransNox);
                        loNewEnt.setEntryNox(lnCtr + 1);
                        loNewEnt.setDateModified(poGRider.getServerDate());
                        lsSQL = MiscUtil.makeSQL((GEntity) loNewEnt, "sBrandNme");
                    }

                    if (!lsSQL.equals("")) {
                        if (poGRider.executeQuery(lsSQL, loNewEnt.getTable(), "", "") == 0) {
                            if (!poGRider.getErrMsg().isEmpty()) {
                                setErrMsg(poGRider.getErrMsg());
                                return false;
                            }
                        }
                    }
                } else {
                    for (int lnCtr2 = lnCtr; lnCtr2 <= laSubUnit.size() - 1; lnCtr2++) {
                        lsSQL = "DELETE FROM " + poDetail.getTable()
                                + " WHERE sStockIDx = " + SQLUtil.toSQL(laSubUnit.get(lnCtr2).getStockIDx())
                                + " AND nEntryNox = " + SQLUtil.toSQL(laSubUnit.get(lnCtr2).getEntryNox());

                        if (!lsSQL.equals("")) {
                            if (poGRider.executeQuery(lsSQL, poDetail.getTable(), "", "") == 0) {
                                if (!poGRider.getErrMsg().isEmpty()) {
                                    setErrMsg(poGRider.getErrMsg());
                                    return false;
                                }
                            }
                        }
                    }
                    break;
                }
            }
            if (lnCtr == laSubUnit.size() - 1) {
                lsSQL = "DELETE FROM " + poDetail.getTable()
                        + " WHERE sStockIDx = " + SQLUtil.toSQL(laSubUnit.get(lnCtr).getStockIDx())
                        + " AND nEntryNox = " + SQLUtil.toSQL(laSubUnit.get(lnCtr).getEntryNox());

                if (!lsSQL.equals("")) {
                    if (poGRider.executeQuery(lsSQL, poDetail.getTable(), "", "") == 0) {
                        if (!poGRider.getErrMsg().isEmpty()) {
                            setErrMsg(poGRider.getErrMsg());
                            return false;
                        }
                    }
                }
            }
        }

        return saveInv(fsTransNox);
    }

    private boolean saveInv(String fsTransNox) {
        setMessage("");

        if (paInv.isEmpty()) {
            setMessage("Unable to save empty detail transaction.");
            return false;
        } else if (paInv.get(0).getStockIDx().equals("")) {
            return true;
        } else if (paInv.get(0).getQtyReqrd() == (Number) 0
                || paInv.get(0).getQtyUsed() == (Number) 0) {
            setMessage("Detail might not have item or zero quantity.");
            return false;
        }

        int lnCtr;
        String lsSQL;
        UnitDailyProductionInv loNewEnt = null;

        if (pnEditMode == EditMode.ADDNEW) {
            Connection loConn = null;
            loConn = setConnection();

            for (lnCtr = 0; lnCtr <= paInv.size() - 1; lnCtr++) {
                loNewEnt = paInv.get(lnCtr);

                if (!loNewEnt.getStockIDx().equals("")) {
                    loNewEnt.setTransNox(fsTransNox);
                    loNewEnt.setEntryNox(lnCtr + 1);
                    loNewEnt.setDateModified(poGRider.getServerDate());
//                    loNewEnt.setDateExpiryDt(paDetail.get(lnCtr).getDateExpiryDt());

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
        } else {
            ArrayList<UnitDailyProductionInv> laSubUnit = loadTransactionInv(poData.getTransNox());

            for (lnCtr = 0; lnCtr <= paInv.size() - 1; lnCtr++) {
                loNewEnt = paInv.get(lnCtr);

                if (!loNewEnt.getStockIDx().equals("")) {
                    if (lnCtr <= laSubUnit.size() - 1) {
                        if (loNewEnt.getEntryNox() != lnCtr + 1) {
                            loNewEnt.setEntryNox(lnCtr + 1);
                        }
                        if (loNewEnt.getTransNox().isEmpty()) {
                            loNewEnt.setTransNox(fsTransNox);
                        }

                        lsSQL = MiscUtil.makeSQL((GEntity) loNewEnt,
                                (GEntity) laSubUnit.get(lnCtr),
                                "sTransNox = " + SQLUtil.toSQL(loNewEnt.getValue(1))
                                + " AND nEntryNox = " + SQLUtil.toSQL(loNewEnt.getValue(2)));

                    } else {
                        loNewEnt.setTransNox(fsTransNox);
                        loNewEnt.setEntryNox(lnCtr + 1);
                        loNewEnt.setDateModified(poGRider.getServerDate());
                        lsSQL = MiscUtil.makeSQL((GEntity) loNewEnt);
                    }

                    if (!lsSQL.equals("")) {
                        if (poGRider.executeQuery(lsSQL, loNewEnt.getTable(), "", "") == 0) {
                            if (!poGRider.getErrMsg().isEmpty()) {
                                setErrMsg(poGRider.getErrMsg());
                                return false;
                            }
                        }
                    }
                } else {
                    for (int lnCtr2 = lnCtr; lnCtr2 <= laSubUnit.size() - 1; lnCtr2++) {
                        lsSQL = "DELETE FROM " + poInv.getTable()
                                + " WHERE sStockIDx = " + SQLUtil.toSQL(laSubUnit.get(lnCtr2).getStockIDx())
                                + " AND nEntryNox = " + SQLUtil.toSQL(laSubUnit.get(lnCtr2).getEntryNox());

                        if (!lsSQL.equals("")) {
                            if (poGRider.executeQuery(lsSQL, poInv.getTable(), "", "") == 0) {
                                if (!poGRider.getErrMsg().isEmpty()) {
                                    setErrMsg(poGRider.getErrMsg());
                                    return false;
                                }
                            }
                        }
                    }
                    break;
                }
            }
            if (lnCtr == laSubUnit.size() - 1) {
                lsSQL = "DELETE FROM " + poDetail.getTable()
                        + " WHERE sStockIDx = " + SQLUtil.toSQL(laSubUnit.get(lnCtr).getStockIDx())
                        + " AND nEntryNox = " + SQLUtil.toSQL(laSubUnit.get(lnCtr).getEntryNox());

                if (!lsSQL.equals("")) {
                    if (poGRider.executeQuery(lsSQL, poDetail.getTable(), "", "") == 0) {
                        if (!poGRider.getErrMsg().isEmpty()) {
                            setErrMsg(poGRider.getErrMsg());
                            return false;
                        }
                    }
                }
            }
        }

        return true;
    }

    public boolean deleteTransaction(String string) {
        UnitDailyProductionMaster loObject = loadTransaction(string);
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

        //delete inventory rows
        lsSQL = "DELETE FROM " + poInv.getTable()
                + " WHERE sTransNox = " + SQLUtil.toSQL(string);

        if (poGRider.executeQuery(lsSQL, poInv.getTable(), "", "") == 0) {
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
        UnitDailyProductionMaster loObject = loadTransaction(string);
        boolean lbResult = false;

        if (loObject == null) {
            setMessage("No record found...");
            return lbResult;
        }

        if (!loObject.getTranStat().equalsIgnoreCase(TransactionStatus.STATE_OPEN)) {
            setMessage("Unable to close closed/cancelled/posted/voided transaction.");
            return lbResult;
        }

        if (poGRider.getUserLevel() < UserRight.SUPERVISOR) {
            setMessage("User is not allowed confirming transaction.");
            return lbResult;
        }

        //check if quantity is served
        for (int lnCtr = 0; lnCtr <= paDetail.size() - 1; lnCtr++) {
            if (Double.valueOf(paDetail.get(lnCtr).getQuantity().toString()) <= 0) {
                setMessage("Detail contains an item with zero quantity.");
                return false;
            }

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
                setErrMsg("No record deleted.");
            }
        } else {
            lbResult = true;
        }

        String lsSourceNo = (String) getMaster("sSourceNo");
        if (lsSourceNo != null && !lsSourceNo.isEmpty()) {
            try {
                ProductionRequest loProductReq = new ProductionRequest(poGRider, poGRider.getBranchCode(), true);
                loProductReq.setTranStat(12340);
                loProductReq.OpenRecord(lsSourceNo);
                loProductReq.PostRecord();

                printTransfer();
            } catch (SQLException ex) {
                Logger.getLogger(DailyProduction.class.getName()).log(Level.SEVERE, null, ex);
            }
        } else {
            /* for saving of detail */
            lbResult = saveInvTrans();
            /* for saving of raw detail */
            if (lbResult) {
                lbResult = saveInvTransSub();
            }
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

    public boolean postTransaction(String string) {
        UnitDailyProductionMaster loObject = loadTransaction(string);
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
        UnitDailyProductionMaster loObject = loadTransaction(string);
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
        UnitDailyProductionMaster loObject = loadTransaction(string);
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

    public boolean SearchDetail(int fnRow, int fnCol, String fsValue, boolean fbSearch, boolean fbByCode) {
//        String lsHeader = "Brand»Description»Unit»e.»Inv. Type»Barcode»Stock ID";
//        String lsColName = "xBrandNme»sDescript»sMeasurNm»xModelNme»xInvTypNm»sBarCodex»sStockIDx";
//        String lsColCrit = "b.sDescript»a.sDescript»f.sMeasurNm»c.sDescript»d.sDescript»a.sBarCodex»a.sStockIDx";
////        
//        String lsHeader = "Barcode»Description»Inv. Type»Brand»Qty on Hand»Stock ID";
//        String lsColName = "a.sBarCodex»sDescript»xInvTypNm»xBrandNme»e.nQtyOnHnd»sStockIDx";
//        String lsColCrit = "a.sBarCodex»a.sDescript»d.sDescript»b.sDescript»e.nQtyOnHnd»a.sStockIDx";

//05/13/2024 revision
        String lsHeader = "Barcode»Description»Brand»Unit»Qty on Hand»Stock ID»Inv. Type";
        String lsColName = "a.sBarCodex»a.sDescript»xBrandNme»f.sMeasurNm»e.nQtyOnHnd»sStockIDx»xInvTypNm";
        String lsColCrit = "a.sBarCodex»a.sDescript»b.sDescript»f.sMeasurNm»e.nQtyOnHnd»a.sStockIDx»d.sDescript";

        String lsSQL = "";
        JSONObject loJSON;
        ResultSet loRS;
        int lnRow;

        setErrMsg("");
        setMessage("");

        switch (fnCol) {
            case 3:
                lsSQL = MiscUtil.addCondition(getSQ_Stocks(), "a.cRecdStat = " + SQLUtil.toSQL(RecordStatus.ACTIVE)
                        + " AND a.sInvTypCd IN " + CommonUtils.getParameter(System.getProperty("store.inventory.type.product")));

                System.out.println(lsSQL);
                if (fbByCode) {
                    if (paDetailOthers.get(fnRow).getValue("sStockIDx").equals(fsValue)) {
                        return true;
                    }

                    lsSQL = MiscUtil.addCondition(lsSQL, "a.sBarCodex = " + SQLUtil.toSQL(fsValue));

                    loRS = poGRider.executeQuery(lsSQL);

                    loJSON = showFXDialog.jsonBrowse(poGRider, loRS, lsHeader, lsColName);
                } else {
                    if (!fbSearch) {
                        if (paDetailOthers.get(fnRow).getValue("sBarCodex").equals(fsValue)) {
                            return true;
                        }

                        loJSON = showFXDialog.jsonSearch(poGRider,
                                lsSQL,
                                fsValue,
                                lsHeader,
                                lsColName,
                                lsColCrit, 1);
                    } else {
                        if (!fsValue.equals("")) {
                            if (paDetailOthers.get(fnRow).getValue("sDescript").equals(fsValue)) {
                                return true;
                            }
                        }

                        loJSON = showFXDialog.jsonSearch(poGRider,
                                lsSQL,
                                fsValue,
                                lsHeader,
                                lsColName,
                                lsColCrit, 1);
                    }
                }

                if (loJSON == null) {
                    setDetail(fnRow, fnCol, "");

                    paDetailOthers.get(fnRow).setValue("sStockIDx", "");
                    paDetailOthers.get(fnRow).setValue("sBarCodex", "");
                    paDetailOthers.get(fnRow).setValue("sDescript", "");
                    paDetailOthers.get(fnRow).setValue("sMeasurNm", "");

                    return false;
                } else {
                    setDetail(fnRow, fnCol, (String) loJSON.get("sStockIDx"));

                    paDetailOthers.get(fnRow).setValue("sStockIDx", (String) loJSON.get("sStockIDx"));
                    paDetailOthers.get(fnRow).setValue("sBarCodex", (String) loJSON.get("sBarCodex"));
                    paDetailOthers.get(fnRow).setValue("sDescript", (String) loJSON.get("sDescript"));
                    paDetailOthers.get(fnRow).setValue("sMeasurNm", (String) loJSON.get("sMeasurNm"));

                    return true;
                }
        }

        return false;
    }

    public boolean SearchDetail(int fnRow, String fsCol, String fsValue, boolean fbSearch, boolean fbByCode) {
        return SearchDetail(fnRow, poDetail.getColumn(fsCol), fsValue, fbSearch, fbByCode);
    }

    public boolean SearchBarcode(int fnCol, String fsValue) {
        String lsHeader = "Barcode»Description»Brand»Unit»Qty on Hand»Stock ID»Inv. Type";
        String lsColName = "a.sBarCodex»a.sDescript»xBrandNme»f.sMeasurNm»e.nQtyOnHnd»sStockIDx»xInvTypNm";

        String lsSQL = "";
        JSONObject loJSON;
        ResultSet loRS;

        setErrMsg("");
        setMessage("");
        if (fnCol == 3){
                lsSQL = MiscUtil.addCondition(getSQ_Stocks(), "a.cRecdStat = " + SQLUtil.toSQL(RecordStatus.ACTIVE)
                        + " AND a.sInvTypCd IN " 
                        + CommonUtils.getParameter(System.getProperty("store.inventory.type.product")));

                System.out.println(lsSQL);

                lsSQL = MiscUtil.addCondition(lsSQL, "a.sBarCodex = " + SQLUtil.toSQL(fsValue));
                loRS = poGRider.executeQuery(lsSQL);

                loJSON = showFXDialog.jsonBrowse(
                        poGRider,
                        loRS,
                        lsHeader,
                        lsColName);

                if (loJSON == null) {
                    return false;
                } else {
                    //check each row if exist
                    for (int lnCtr = 0; lnCtr < ItemCount(); lnCtr++) {
                        if (paDetailOthers.get(lnCtr).getValue("sBarCodex").equals(fsValue)) {
                            //auto add qty
                            setDetail(lnCtr, "nQuantity", (Double) paDetail.get(lnCtr).getQuantity() + 1.0);
                            return true;
                        }
                    }

                    addDetail();
                    int lnCount = ItemCount() - 1;
                    setDetail(lnCount, fnCol, (String) loJSON.get("sStockIDx"));
                    paDetailOthers.get(lnCount).setValue("sStockIDx", (String) loJSON.get("sStockIDx"));
                    paDetailOthers.get(lnCount).setValue("sBarCodex", (String) loJSON.get("sBarCodex"));
                    paDetailOthers.get(lnCount).setValue("sDescript", (String) loJSON.get("sDescript"));
                    paDetailOthers.get(lnCount).setValue("sMeasurNm", (String) loJSON.get("sMeasurNm"));
                    setDetail(lnCount, "nQuantity", (Double) paDetail.get(lnCount).getQuantity() + 1.0);

                    return true;
                }
        }

        return false;
    }

    public boolean SearchBarcode(String fsCol, String fsValue) {
        return SearchBarcode(poDetail.getColumn(fsCol), fsValue);
    }

    public boolean SearchInv(int fnRow, int fnCol, String fsValue, boolean fbSearch, boolean fbByCode) {
//        String lsHeader = "Barcode»Description»Inv. Type»Brand»Qty on Hand»Stock ID";
//        String lsColName = "a.sBarCodex»sDescript»xInvTypNm»xBrandNme»e.nQtyOnHnd»sStockIDx";
//        String lsColCrit = "a.sBarCodex»a.sDescript»d.sDescript»b.sDescript»e.nQtyOnHnd»a.sStockIDx";

//05/13/2024 revision
        String lsHeader = "Barcode»Description»Brand»Unit»Qty on Hand»Stock ID»Inv. Type";
        String lsColName = "a.sBarCodex»a.sDescript»xBrandNme»f.sMeasurNm»e.nQtyOnHnd»sStockIDx»xInvTypNm";
        String lsColCrit = "a.sBarCodex»a.sDescript»b.sDescript»f.sMeasurNm»e.nQtyOnHnd»a.sStockIDx»d.sDescript";
        String lsSQL = "";
        JSONObject loJSON;
        ResultSet loRS;
        int lnRow;

        setErrMsg("");
        setMessage("");

        switch (fnCol) {
            case 3:
                lsSQL = MiscUtil.addCondition(getStocksWExpiraiton(), "a.cRecdStat = " + SQLUtil.toSQL(RecordStatus.ACTIVE));

                if (fbByCode) {
                    if (paInvOthers.get(fnRow).getValue("sStockIDx").equals(fsValue)) {
                        return true;
                    }

                    lsSQL = MiscUtil.addCondition(lsSQL, "a.sBarCodex = " + SQLUtil.toSQL(fsValue));

                    loRS = poGRider.executeQuery(lsSQL);

                    loJSON = showFXDialog.jsonBrowse(poGRider, loRS, lsHeader, lsColName);
                } else {
                    if (!fbSearch) {
                        if (paInvOthers.get(fnRow).getValue("sBarCodex").equals(fsValue) && !fsValue.isEmpty()) {
                            return true;
                        }

                        loJSON = showFXDialog.jsonSearch(poGRider,
                                lsSQL,
                                fsValue,
                                lsHeader,
                                lsColName,
                                lsColCrit, 0);
                    } else {
                        if (paInvOthers.get(fnRow).getValue("sDescript").equals(fsValue) && !fsValue.isEmpty()) {
                            return true;
                        }

                        loJSON = showFXDialog.jsonSearch(poGRider,
                                lsSQL,
                                fsValue,
                                lsHeader,
                                lsColName,
                                lsColCrit, 1);
                    }
                }

                if (loJSON == null) {
                    setDetail(fnRow, fnCol, "");
                    paInvOthers.get(fnRow).setValue("sStockIDx", "");
                    paInvOthers.get(fnRow).setValue("sBarCodex", "");
                    paInvOthers.get(fnRow).setValue("sDescript", "");
                    paInvOthers.get(fnRow).setValue("sMeasurNm", "");
                    paInvOthers.get(fnRow).setValue("sBrandNme", "");

                    return false;
                } else {
                    setInv(fnRow, fnCol, (String) loJSON.get("sStockIDx"));

                    paInvOthers.get(fnRow).setValue("sStockIDx", (String) loJSON.get("sStockIDx"));
                    paInvOthers.get(fnRow).setValue("sBarCodex", (String) loJSON.get("sBarCodex"));
                    paInvOthers.get(fnRow).setValue("sDescript", (String) loJSON.get("sDescript"));
                    paInvOthers.get(fnRow).setValue("sMeasurNm", (String) loJSON.get("sMeasurNm"));
                    paInvOthers.get(fnRow).setValue("sBrandNme", (String) loJSON.get("xBrandNme"));

                    return true;
                }
        }

        return false;
    }

    public boolean SearchInv(int fnRow, String fsCol, String fsValue, boolean fbSearch, boolean fbByCode) {
        return SearchInv(fnRow, poInv.getColumn(fsCol), fsValue, fbSearch, fbByCode);
    }

    public boolean SearchBarcodeRaw(int fnCol, String fsValue) {
        String lsHeader = "Barcode»Description»Brand»Unit»Qty on Hand»Stock ID»Inv. Type";
        String lsColName = "a.sBarCodex»a.sDescript»xBrandNme»f.sMeasurNm»e.nQtyOnHnd»sStockIDx»xInvTypNm";
        String lsColCrit = "a.sBarCodex»a.sDescript»b.sDescript»f.sMeasurNm»e.nQtyOnHnd»a.sStockIDx»d.sDescript";
        String lsSQL = "";
        JSONObject loJSON;
        ResultSet loRS;
        int lnRow;

        setErrMsg("");
        setMessage("");
        if (fnCol == 3){
                lsSQL = MiscUtil.addCondition(getStocksWExpiraiton(), "a.cRecdStat = " + SQLUtil.toSQL(RecordStatus.ACTIVE));

                lsSQL = MiscUtil.addCondition(lsSQL, "a.sBarCodex = " + SQLUtil.toSQL(fsValue));
                loRS = poGRider.executeQuery(lsSQL);
                loJSON = showFXDialog.jsonBrowse(
                        poGRider,
                        loRS,
                        lsHeader,
                        lsColName);

                if (loJSON == null) {
                    return false;
                } else {
                    //check each row if exist
                    for (int lnCtr = 0; lnCtr < ItemCount(); lnCtr++) {
                        if (paInvOthers.get(lnCtr).getValue("sBarCodex").equals(fsValue)) {
                            //auto add qty
                            setInv(lnCtr, "nQtyReqrd", (Double) paInv.get(lnCtr).getQtyReqrd() + 1.0);
                            setInv(lnCtr, "nQtyUsedx", (Double) paInv.get(lnCtr).getQtyUsed() + 1.0);
                            return true;
                        }
                    }

                    addInv();
                    int lnCount = InvCount() - 1;

                    setInv(lnCount, fnCol, (String) loJSON.get("sStockIDx"));

                    paInvOthers.get(lnCount).setValue("sStockIDx", (String) loJSON.get("sStockIDx"));
                    paInvOthers.get(lnCount).setValue("sBarCodex", (String) loJSON.get("sBarCodex"));
                    paInvOthers.get(lnCount).setValue("sDescript", (String) loJSON.get("sDescript"));
                    paInvOthers.get(lnCount).setValue("sMeasurNm", (String) loJSON.get("sMeasurNm"));
                    paInvOthers.get(lnCount).setValue("sBrandNme", (String) loJSON.get("xBrandNme"));
                    setInv(lnCount, "nQtyReqrd", (Double) paInv.get(lnCount).getQtyReqrd() + 1.0);
                    setInv(lnCount, "nQtyUsedx", (Double) paInv.get(lnCount).getQtyUsed() + 1.0);

                    return true;
                }
        }

        return false;
    }

    public boolean SearchBarcodeRaw(String fsCol, String fsValue) {
        return SearchBarcodeRaw(poInv.getColumn(fsCol), fsValue);
    }

    private boolean saveInvTrans() {
        InventoryTrans loInvTrans = new InventoryTrans(poGRider, poGRider.getBranchCode());
        loInvTrans.InitTransaction();

        ResultSet loRS = null;
        String lsSQL = "";

        for (int lnCtr = 0; lnCtr <= paDetail.size() - 1; lnCtr++) {
            if (paDetail.get(lnCtr).getStockIDx().equals("")) {
                break;
            }

            loInvTrans.setDetail(lnCtr, "sStockIDx", paDetail.get(lnCtr).getStockIDx());
            loInvTrans.setDetail(lnCtr, "nQuantity", paDetail.get(lnCtr).getQuantity());
            System.out.println("LEDGER1 :" + paDetail.get(lnCtr).getDateExpiryDt());
            loInvTrans.setDetail(lnCtr, "dExpiryDt", paDetail.get(lnCtr).getDateExpiryDt());

            lsSQL = "SELECT"
                    + "  nQtyOnHnd"
                    + ", nResvOrdr"
                    + ", nBackOrdr"
                    + ", nLedgerNo"
                    + " FROM Inv_Master"
                    + " WHERE sStockIDx = " + SQLUtil.toSQL(paDetail.get(lnCtr).getStockIDx())
                    + " AND sBranchCd = " + SQLUtil.toSQL(psBranchCd);

            loRS = poGRider.executeQuery(lsSQL);
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

        if (!loInvTrans.DailyProduction_IN(poData.getTransNox(), poGRider.getServerDate(), EditMode.ADDNEW)) {
            setMessage(loInvTrans.getMessage());
            setErrMsg(loInvTrans.getErrMsg());
            return false;
        }

        //TODO
        //update branch order info
        return saveInvExpiration(poData.getDateTransact(), true);
    }

    private boolean unsaveInvTrans() {
        InventoryTrans loInvTrans = new InventoryTrans(poGRider, poGRider.getBranchCode());
        loInvTrans.InitTransaction();

        for (int lnCtr = 0; lnCtr <= paDetail.size() - 1; lnCtr++) {
            loInvTrans.setDetail(lnCtr, "sStockIDx", paDetail.get(lnCtr).getStockIDx());
            loInvTrans.setDetail(lnCtr, "nQtyOnHnd", paDetailOthers.get(lnCtr).getValue("nQtyOnHnd"));
            loInvTrans.setDetail(lnCtr, "nResvOrdr", paDetailOthers.get(lnCtr).getValue("nResvOrdr"));
            loInvTrans.setDetail(lnCtr, "nLedgerNo", paDetailOthers.get(lnCtr).getValue("nLedgerNo"));
        }

        if (!loInvTrans.DailyProduction_IN(poData.getTransNox(), poGRider.getServerDate(), EditMode.DELETE)) {
            setMessage(loInvTrans.getMessage());
            setErrMsg(loInvTrans.getErrMsg());
            return false;
        }

        //TODO
        //update branch order info
        return true;
    }

    private boolean saveInvTransSub() {
        InventoryTrans loInvTrans = new InventoryTrans(poGRider, poGRider.getBranchCode());
        loInvTrans.InitTransaction();

        ResultSet loRS = null;
        String lsSQL = "";

        for (int lnCtr = 0; lnCtr <= paInv.size() - 1; lnCtr++) {
            if (paInv.get(lnCtr).getStockIDx().equals("")) {
                break;
            }

            loInvTrans.setDetail(lnCtr, "sStockIDx", paInv.get(lnCtr).getStockIDx());
            //jovan part of debugging for raw data not saving correctly for posting
            //loInvTrans.setDetail(lnCtr, "nQtyUsedx", paInv.get(lnCtr).getQtyUsed());
            loInvTrans.setDetail(lnCtr, "nQuantity", paInv.get(lnCtr).getQtyUsed());

            lsSQL = "SELECT"
                    + "  nQtyOnHnd"
                    + ", nResvOrdr"
                    + ", nBackOrdr"
                    + ", nLedgerNo"
                    + " FROM Inv_Master"
                    + " WHERE sStockIDx = " + SQLUtil.toSQL(paInv.get(lnCtr).getStockIDx())
                    + " AND sBranchCd = " + SQLUtil.toSQL(psBranchCd);

            loRS = poGRider.executeQuery(lsSQL);
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
        if (!loInvTrans.DailyProduction_OUT(poData.getTransNox(), poGRider.getServerDate(), EditMode.ADDNEW)) {
            setMessage(loInvTrans.getMessage());
            setErrMsg(loInvTrans.getErrMsg());
            return false;
        }

        //TODO
        //update branch order info
        return saveInvExpirationSub(poData.getDateTransact());
    }

    private boolean unsaveInvTransSub() {
        InventoryTrans loInvTrans = new InventoryTrans(poGRider, poGRider.getBranchCode());
        loInvTrans.InitTransaction();

        for (int lnCtr = 0; lnCtr <= paInv.size() - 1; lnCtr++) {
            loInvTrans.setDetail(lnCtr, "sStockIDx", paInv.get(lnCtr).getStockIDx());
            loInvTrans.setDetail(lnCtr, "nQtyOnHnd", paInvOthers.get(lnCtr).getValue("nQtyOnHnd"));
            loInvTrans.setDetail(lnCtr, "nResvOrdr", paInvOthers.get(lnCtr).getValue("nResvOrdr"));
            loInvTrans.setDetail(lnCtr, "nLedgerNo", paInvOthers.get(lnCtr).getValue("nLedgerNo"));
        }

        if (!loInvTrans.DailyProduction_OUT(poData.getTransNox(), poGRider.getServerDate(), EditMode.DELETE)) {
            setMessage(loInvTrans.getMessage());
            setErrMsg(loInvTrans.getErrMsg());
            return false;
        }

        //TODO
        //update branch order info
        return true;
    }

    private boolean saveInvExpiration(Date fdTransact, Boolean fbInvIn) {
//        InvExpiration loInvTrans = new InvExpiration(poGRider, poGRider.getBranchCode());
//        loInvTrans.InitTransaction();
//
//        boolean lbProcess = false;
//
//        if (fbInvIn == false) {
//            for (int lnCtr = 0; lnCtr <= paInv.size() - 1; lnCtr++) {
//                if (paInv.get(lnCtr).getStockIDx().equals("")) {
//                    break;
//                }
//                loInvTrans.setDetail(lnCtr, "sStockIDx", paInv.get(lnCtr).getStockIDx());
//                loInvTrans.setDetail(lnCtr, "dExpiryDt", paInv.get(lnCtr).getDateExpiryDt());
//                loInvTrans.setDetail(lnCtr, "nQtyOutxx", paInv.get(lnCtr).getQtyUsed());
//
//                lbProcess = true;
//            }
//        } else {
//            for (int lnCtr = 0; lnCtr <= paDetail.size() - 1; lnCtr++) {
//                if (paDetail.get(lnCtr).getStockIDx().equals("")) {
//                    break;
//                }
//                loInvTrans.setDetail(lnCtr, "sStockIDx", paDetail.get(lnCtr).getStockIDx());
//                loInvTrans.setDetail(lnCtr, "dExpiryDt", paDetail.get(lnCtr).getDateExpiryDt());
//                loInvTrans.setDetail(lnCtr, "nQtyInxxx", paDetail.get(lnCtr).getQuantity());
//
//                lbProcess = true;
//            }
//        }
//
//        if (fbInvIn == true) {
//            if (lbProcess) {
//                if (!loInvTrans.DailyProduction_IN(fdTransact, EditMode.ADDNEW)) {
//                    setMessage(loInvTrans.getMessage());
//                    setErrMsg(loInvTrans.getErrMsg());
//                    return false;
//                }
//            }
//        } else {
//            return saveInvExpirationSub(fdTransact);
//        }

        return true;
    }

    private boolean saveInvExpirationSub(Date fdTransact) {
        String lsSQL;
        ResultSet loRS;
        int lnRow;
        int lnTemp;
        double lnTempQTY;
        boolean lbProcess = false;

        InvExpiration loInvTrans = new InvExpiration(poGRider, poGRider.getBranchCode());
        loInvTrans.InitTransaction();

        lnTemp = 0;
        for (int lnCtr = 0; lnCtr <= paInv.size() - 1; lnCtr++) {
            if (paInv.get(lnCtr).getStockIDx().equals("")) {
                break;
            }

            lsSQL = "SELECT"
                    + "  sStockIDx"
                    + ", dExpiryDt"
                    + ", nQtyOnHnd"
                    + " FROM Inv_Master_Expiration"
                    + " WHERE sStockIDx = " + SQLUtil.toSQL(paInv.get(lnCtr).getStockIDx())
                    + " AND sBranchCd = " + SQLUtil.toSQL(psBranchCd)
                    + " AND nQtyOnHnd > 0"
                    + " ORDER BY dExpiryDt ASC";

            loRS = poGRider.executeQuery(lsSQL);

            try {
                if (MiscUtil.RecordCount(loRS) == 0) {
                    loInvTrans.setDetail(lnTemp, "nQtyOutxx", paInv.get(lnCtr).getQtyUsed());
                    loInvTrans.setDetail(lnTemp, "sStockIDx", paInv.get(lnCtr).getStockIDx());
                    loInvTrans.setDetail(lnTemp, "dExpiryDt", fdTransact);
                } else {
                    lnTempQTY = Double.valueOf(paInv.get(lnCtr).getQtyUsed().toString());
                    loRS.first();

                    for (lnRow = 0; lnRow <= MiscUtil.RecordCount(loRS) - 1; lnRow++) {
                        if (lnTempQTY <= loRS.getInt("nQtyOnHnd")) {
                            loInvTrans.setDetail(lnTemp, "nQtyOutxx", lnTempQTY);
                            loInvTrans.setDetail(lnTemp, "sStockIDx", paInv.get(lnCtr).getStockIDx());
                            loInvTrans.setDetail(lnTemp, "dExpiryDt", loRS.getDate("dExpiryDt"));

                            lnTemp++;
                            break;
                        } else {
                            loInvTrans.setDetail(lnTemp, "nQtyOutxx", loRS.getInt("nQtyOnHnd"));
                            loInvTrans.setDetail(lnTemp, "sStockIDx", paInv.get(lnCtr).getStockIDx());
                            loInvTrans.setDetail(lnTemp, "dExpiryDt", loRS.getDate("dExpiryDt"));

                            lnTempQTY = lnTempQTY - loRS.getInt("nQtyOnHnd");
                        }

                        lnTemp++;
                        loRS.next();
                        lbProcess = true;
                    }
                }
            } catch (SQLException ex) {
                Logger.getLogger(InvTransfer.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        if (lbProcess) {
            if (!loInvTrans.DailyProduction_OUT(fdTransact, EditMode.ADDNEW)) {
                setMessage(loInvTrans.getMessage());
                setErrMsg(loInvTrans.getErrMsg());
                return false;
            }
        }

        return true;
    }

    public void setMaster(int fnCol, Object foData) {
        if (pnEditMode != EditMode.UNKNOWN) {
            // Don't allow specific fields to assign values
            if (!(fnCol == poData.getColumn("sTransNox")
                    || fnCol == poData.getColumn("nEntryNox")
                    || fnCol == poData.getColumn("cTranStat")
                    || fnCol == poData.getColumn("sModified")
                    || fnCol == poData.getColumn("dModified"))) {

                poData.setValue(fnCol, foData);
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
        return MiscUtil.makeSelect(new UnitDailyProductionMaster());
    }

    private String getSQ_Detail() {
        return "SELECT"
                + "  a.sTransNox"
                + ", a.nEntryNox"
                + ", a.sStockIDx"
                + ", a.nGoalQtyx"
                + ", a.nOrderQty"
                + ", a.nQuantity"
                + ", a.dExpiryDt"
                + ", a.dModified"
                + ", c.sBarCodex"
                + ", c.sDescript"
                + ", d.sMeasurNm"
                + ", IFNULL(e.sDescript, '') xBrandNme"
                + " FROM Daily_Production_Detail a"
                + ", Inv_Master b"
                + " LEFT JOIN Inventory c"
                + " ON b.sStockIDx = c.sStockIDx"
                + " LEFT JOIN Brand e"
                + " ON c.sBrandCde = e.sBrandCde"
                + " LEFT JOIN Measure d"
                + " ON c.sMeasurID = d.sMeasurID"
                + " WHERE a.sStockIDx = b.sStockIDx"
                + " AND b.sBranchCD = " + SQLUtil.toSQL(poGRider.getBranchCode());
    }

    private String getSQ_Inv() {
        return "SELECT"
                + "  a.sTransNox"
                + ", a.nEntryNox"
                + ", a.sStockIDx"
                + ", a.nQtyReqrd"
                + ", IFNULL(a.nQtyUsedx,'0') nQtyUsedx"
                + ", a.dExpiryDt"
                + ", a.dModified"
                + ", c.sBarCodex"
                + ", c.sDescript"
                + ", IFNULL(e.sMeasurNm, 'NONE') sMeasurNm"
                + ", IFNULL(d.sDescript,'') xBrandNme  "
                + " FROM Daily_Production_Inventory a"
                + ", Inv_Master b"
                + " LEFT JOIN Inventory c"
                + " ON b.sStockIDx = c.sStockIDx"
                + "   LEFT JOIN Brand d  "
                + "      ON c.sBrandCde = d.sBrandCde  "
                + "  LEFT JOIN Measure e"
                + "      ON c.sMeasurID = e.sMeasurID"
                + " WHERE a.sStockIDx = b.sStockIDx"
                + " AND b.sBranchCD = " + SQLUtil.toSQL(poGRider.getBranchCode());
    }

    public int ItemCount() {
        return paDetail.size();
    }

    public int InvCount() {
        return paInv.size();
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

    private String getSQ_DailyProducton() {
        String lsTranStat = String.valueOf(pnTranStat);
        String lsCondition = "";
        String lsSQL = "SELECT "
                + "  a.sTransNox"
                + ",DATE_FORMAT(a.dTransact, '%m/%d/%Y') AS dTransact"
                + ", a.sRemarksx"
                + " FROM Daily_Production_Master a"
                + " WHERE a.sTransNox LIKE " + SQLUtil.toSQL(psBranchCd + "%");

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
                + ", f.sMeasurNm"
                + ", e.nQtyOnHnd"
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
                + " AND a.sInvTypCd IN ('FsGd', 'PREC')"
                + " AND e.sBranchCd = " + SQLUtil.toSQL(psBranchCd);

        return lsSQL;
    }

    private String getStocksWExpiraiton() {
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
                + ", f.sMeasurNm"
                + ", e.nQtyOnHnd"
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
                + ", Inv_Master_Expiration g"
                + " WHERE a.sStockIDx = e.sStockIDx"
                + " AND e.sStockIDx = g.sStockIDx "
                + " AND a.sInvTypCd = 'RwMt'"
                + " AND e.sBranchCd = " + SQLUtil.toSQL(psBranchCd)
                + " GROUP BY a.sStockIDx";

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

    public boolean printTransfer() {
        if (poData == null) {
            ShowMessageFX.Warning("Unable to print transaction.", "Warning", "No record loaded.");
            return false;
        }

        // Create the parameters for the report
        Map<String, Object> params = new HashMap<>();
        params.put("sBranchNm", poGRider.getBranchName());
        params.put("sAddressx", poGRider.getAddress() + ", " + poGRider.getTownName() + " " + poGRider.getProvince());
        params.put("sTransNox", poData.getTransNox());
        params.put("sSourceNo", poData.getSourceNo());
        params.put("dTransact", SQLUtil.dateFormat(poData.getDateTransact(), SQLUtil.FORMAT_LONG_DATE));
        params.put("sPrintdBy", poGRider.getClientName());
        params.put("xRemarksx", poData.getRemarksx());

        JSONObject loJSON;

        JSONArray loArray = new JSONArray();
        try {
            String lsSQL = MiscUtil.addCondition("SELECT a.sTransNox, b.sBranchCd FROM Inv_Stock_Request_Detail a "
                    + " LEFT JOIN Inv_Stock_Request_Master b "
                    + " ON a.sTransNox = b.sTransNox "
                    + " GROUP BY a.sTransNox", "a.sBatchNox = " + SQLUtil.toSQL(poData.getSourceNo()));
            System.err.println(lsSQL);
            ResultSet loRS = poGRider.executeQuery(lsSQL);

            if (!loRS.isBeforeFirst()) { // This checks if there are no rows in the ResultSet
                loRS.close(); // Always close your ResultSet

                System.err.println("No Result");
                return false; // Return false if no records found
            }
            System.err.println("Fetching Data");
            while (loRS.next()) {
                InvRequest instance = new InvRequest(poGRider, psBranchCd, !pbWithParent);
                String lsBranchName = "";

                System.err.println(loRS.getString("sTransNox"));
                if (instance.openTransaction(loRS.getString("sTransNox"))) {
                    XMBranch loBranch = instance.GetBranch(loRS.getString("sBranchCd"), true);
                    if (loBranch != null) {
                        lsBranchName = (String) loBranch.getMaster("sBranchNm");
                    }
                    // Open the transaction to get item details
                    for (int lnItemRow = 0; lnItemRow <= instance.ItemCount() - 1; lnItemRow++) {
                        String lsBarCodex = (String) instance.getDetailOthers(lnItemRow, "sBarCodex");
                        String lsDescript = (String) instance.getDetailOthers(lnItemRow, "sDescript");
                        String lsMeasurex = (String) instance.getDetailOthers(lnItemRow, "sMeasurNm");
                        double lnQuantity = Double.valueOf(instance.getDetail(lnItemRow, "nQuantity").toString());

                        // Create a JSON object for the current item
                        loJSON = new JSONObject();
                        loJSON.put("sField01", lsBranchName);
                        loJSON.put("sField02", lsBarCodex);
                        loJSON.put("sField03", lsDescript);
                        loJSON.put("sField04", lsMeasurex);
                        loJSON.put("nField01", lnQuantity);

                        // Add the JSON object to the array
                        loArray.add(loJSON);
                    }
                }
            }

            System.err.println("Finish Fetching Data");
            loRS.close();

            // Convert the JSON array to InputStream
            InputStream stream = new ByteArrayInputStream(loArray.toJSONString().getBytes("UTF-8"));
            JsonDataSource jrjson = new JsonDataSource(stream);

            // Generate the report
            JasperPrint jrprint = JasperFillManager.fillReport(
                    poGRider.getReportPath() + "DailyProductionCollation.jasper", params, jrjson);

            // Show the report in a viewer
            JasperViewer jv = new JasperViewer(jrprint, false);
            jv.setVisible(true);
            jv.setAlwaysOnTop(true);
        } catch (JRException | UnsupportedEncodingException | SQLException ex) {
//            Logger.getLogger(DailyProduction.class.getName()).log(Level.SEVERE, null, ex);
            return false;
        }

        return true;
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

    private UnitDailyProductionMaster poData = new UnitDailyProductionMaster();
    private UnitDailyProductionDetail poDetail = new UnitDailyProductionDetail();
    private UnitDailyProductionInv poInv = new UnitDailyProductionInv();
    private ArrayList<UnitDailyProductionDetail> paDetail;
    private ArrayList<UnitDailyProductionInv> paInv;
    private ArrayList<UnitDailyProductionDetailOthers> paDetailOthers;
    private ArrayList<UnitDailyProductionInvOthers> paInvOthers;
}
