package org.rmj.cas.inventory.base;

import com.mysql.jdbc.Connection;
import com.sun.scenario.effect.impl.Renderer;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.Date;
import org.json.simple.JSONObject;
import org.rmj.appdriver.constants.EditMode;
import org.rmj.appdriver.constants.RecordStatus;
import org.rmj.appdriver.GRider;
import org.rmj.appdriver.iface.GEntity;
import org.rmj.appdriver.MiscUtil;
import org.rmj.appdriver.SQLUtil;
import org.rmj.appdriver.agentfx.CommonUtils;
import org.rmj.appdriver.agentfx.callback.IResult;
import org.rmj.appdriver.agentfx.ui.showFXDialog;
import org.rmj.cas.inventory.pojo.UnitInvMaster;
import org.rmj.cas.inventory.pojo.UnitInvSerial;
import org.rmj.cas.inventory.pojo.UnitInventory;
import org.rmj.lp.parameter.agent.XMBrand;
import org.rmj.lp.parameter.agent.XMCategory;
import org.rmj.lp.parameter.agent.XMCategoryLevel2;
import org.rmj.lp.parameter.agent.XMCategoryLevel3;
import org.rmj.lp.parameter.agent.XMCategoryLevel4;
import org.rmj.lp.parameter.agent.XMColor;
import org.rmj.lp.parameter.agent.XMInventoryType;
import org.rmj.lp.parameter.agent.XMMeasure;
import org.rmj.lp.parameter.agent.XMModel;
import org.rmj.lp.parameter.agent.XMInventoryLocation;

/**
 * Inventory Master BASE
 *
 * @author Michael Torres Cuison
 * @since 2018.10.05
 */
public class InvMaster {

    public InvMaster(GRider foGRider, String fsBranchCD, boolean fbWithParent) {
        this.poGRider = foGRider;

        if (foGRider != null) {
            this.pbWithParent = fbWithParent;
            this.psBranchCd = fsBranchCD;

            this.psUserIDxx = foGRider.getUserID();
            pnEditMode = EditMode.UNKNOWN;

            poInventory = new Inventory(poGRider, psBranchCd, true);
        }
    }

    public void setCallback(IResult foValue) {
        poCallback = foValue;
    }

    public boolean SearchSoldStock(String fsValue, String fsSerialID, boolean fbSearch, boolean fbByCode) {
        String lsHeader = "Refer No.»Description»Unit»Model»Brand"; //On Hand»
        String lsColName = "xReferNox»sDescript»sMeasurNm»xModelNme»xBrandNme"; //nQtyOnHnd»       
        String lsSQL = "SELECT * FROM (" + getSQ_SoldStock() + ") a";

        JSONObject loJSON;
        ResultSet loRS;

        if (fbByCode) {
            if (fsSerialID.equals("")) {
                lsSQL = lsSQL + " WHERE a.sStockIDx = " + SQLUtil.toSQL(fsValue);
            } //lsSQL = lsSQL.replace("xCondition", "a.sStockIDx = " + SQLUtil.toSQL(fsValue));
            else {
                lsSQL = lsSQL + " WHERE a.sSerialID = " + SQLUtil.toSQL(fsSerialID);
            }
            // lsSQL = lsSQL.replace("xCondition", "a.sSerialID = " + SQLUtil.toSQL(fsValue));
        } else {
            //lsSQL = lsSQL.replace("xCondition", "a.sDescript LIKE " + SQLUtil.toSQL(fsValue + "%"));
            lsSQL = lsSQL + " WHERE a.sDescript LIKE " + SQLUtil.toSQL(fsValue + "%");
        }

        loRS = poGRider.executeQuery(lsSQL);
        long lnRow = MiscUtil.RecordCount(loRS);

        if (lnRow == 0) {
            pnEditMode = EditMode.UNKNOWN;
            return false;
        } else if (lnRow == 1) {
            loJSON = CommonUtils.loadJSON(loRS);
        } else {
            loJSON = showFXDialog.jsonBrowse(poGRider, loRS, lsHeader, lsColName);
        }

        if (loJSON != null) {
            psStockIDx = (String) loJSON.get("sStockIDx");
            psBarCodex = (String) loJSON.get("sBarCodex");
            psDescript = (String) loJSON.get("sDescript");

            if ("1".equals((String) loJSON.get("cSerialze"))) {
                psSerialID = (String) loJSON.get("sSerialID");
                psSerial01 = (String) loJSON.get("xReferNox");
                psSerial02 = (String) loJSON.get("xReferNo1");
            }
        } else {
            psStockIDx = "";
            psBarCodex = "";
            psDescript = "";
            psSerialID = "";
            psSerial01 = "";
            psSerial02 = "";

            psWarnMsg = "No record found/selected. Please verify your entry.";
            psErrMsgx = "";
        }

        if (openInvRecord(psStockIDx)) {
            poData = openRecord(psStockIDx);

            if (!psSerialID.trim().equals("")) {
                poSerial = openSerial(psSerialID);
            }

            if (poData == null && poSerial == null) {
                return NewRecord();
            } else {
                return true;
            }
        } else {
            pnEditMode = EditMode.UNKNOWN;
            return false;
        }
    }

    public boolean SearchStock(String fsValue, String fsSerialID, boolean fbSearch, boolean fbByCode) {
//        String lsHeader = "Refer No.»Description»Unit»Model»Brand"; //On Hand»
//        String lsColName = "xReferNox»sDescript»sMeasurNm»xModelNme»xBrandNme"; //nQtyOnHnd» 

        String lsHeader = "Refer No.»Description»Brand»Unit»Qty on Hand»Model"; //On Hand»
        String lsColName = "xReferNox»sDescript»xBrandNme»sMeasurNm»nQtyOnHnd»xModelNme"; //nQtyOnHnd» 
        String lsSQL = "SELECT * FROM (" + getSQ_AllStock() + ") a";

        JSONObject loJSON;
        ResultSet loRS;

        if (fbByCode) {
            if (fsSerialID.equals("")) {
                lsSQL = lsSQL + " WHERE a.sStockIDx = " + SQLUtil.toSQL(fsValue);
            } //lsSQL = lsSQL.replace("xCondition", "a.sStockIDx = " + SQLUtil.toSQL(fsValue));
            else {
                lsSQL = lsSQL + " WHERE a.sSerialID = " + SQLUtil.toSQL(fsSerialID);
            }
            // lsSQL = lsSQL.replace("xCondition", "a.sSerialID = " + SQLUtil.toSQL(fsValue));
        } else {
            //lsSQL = lsSQL.replace("xCondition", "a.sDescript LIKE " + SQLUtil.toSQL(fsValue + "%"));
            lsSQL = lsSQL + " WHERE a.sDescript LIKE " + SQLUtil.toSQL(fsValue + "%");
        }

        loRS = poGRider.executeQuery(lsSQL);
        long lnRow = MiscUtil.RecordCount(loRS);

        if (lnRow == 0) {
            pnEditMode = EditMode.UNKNOWN;
            return false;
        } else if (lnRow == 1) {
            loJSON = CommonUtils.loadJSON(loRS);
        } else {
            loJSON = showFXDialog.jsonBrowse(poGRider, loRS, lsHeader, lsColName);
        }

        if (loJSON != null) {
            psStockIDx = (String) loJSON.get("sStockIDx");
            psBarCodex = (String) loJSON.get("sBarCodex");
            psDescript = (String) loJSON.get("sDescript");

            if ("1".equals((String) loJSON.get("cSerialze"))) {
                psSerialID = (String) loJSON.get("sSerialID");
                psSerial01 = (String) loJSON.get("xReferNox");
                psSerial02 = (String) loJSON.get("xReferNo1");
            }
        } else {
            psStockIDx = "";
            psBarCodex = "";
            psDescript = "";
            psSerialID = "";
            psSerial01 = "";
            psSerial02 = "";

            psWarnMsg = "No record found/selected. Please verify your entry.";
            psErrMsgx = "";
        }

        if (openInvRecord(psStockIDx)) {
            poData = openRecord(psStockIDx);

            if (!psSerialID.trim().equals("")) {
                poSerial = openSerial(psSerialID);
            }

            if (poData == null && poSerial == null) {
                return NewRecord();
            } else {
                return true;
            }
        } else {
            pnEditMode = EditMode.UNKNOWN;
            return false;
        }
    }

    /**
     * SearchStock(String fsCategCd1, String fsValue, boolean fbSearch, boolean
     * fbByCode)
     *
     * @param fsCategCd1 category level 1
     * @param fsValue value to search
     * @param fbSearch search or browse
     * @param fbByCode by code or description
     * @return boolean
     */
    public boolean SearchStockByCategory(String fsCategCd1, String fsValue, boolean fbSearch, boolean fbByCode) {
        String lsHeader = "Brand»Description»Unit»Model»On Hand»Inv. Type»Barcode»Stock ID";
        String lsColName = "xBrandNme»sDescript»sMeasurNm»xModelNme»nQtyOnHnd»xInvTypNm»sBarCodex»sStockIDx";
        String lsColCrit = "b.sDescript»a.sDescript»e.sMeasurNm»c.sDescript»f.nQtyOnHndd.sDescript»a.sBarCodex»a.sStockIDx";
        String lsSQL = MiscUtil.addCondition(getSQ_Stock(), "a.cRecdStat = " + SQLUtil.toSQL(RecordStatus.ACTIVE)
                + " AND f.nQtyOnHnd > 0");

        if (!fsCategCd1.equals("")) {
            lsSQL = MiscUtil.addCondition(lsSQL, "a.sCategCd1 = " + SQLUtil.toSQL(fsCategCd1));
        }

        JSONObject loJSON;

        if (fbByCode) {
            lsSQL = MiscUtil.addCondition(lsSQL, "a.sStockIDx = " + SQLUtil.toSQL(fsValue));

            ResultSet loRS = poGRider.executeQuery(lsSQL);
            if (MiscUtil.RecordCount(loRS) < 1) {
                pnEditMode = EditMode.UNKNOWN;
                return false;
            }

            loJSON = CommonUtils.loadJSON(loRS);
            //loJSON = showFXDialog.jsonBrowse(poGRider, loRS, lsHeader, lsColName);
        } else {
            loJSON = showFXDialog.jsonSearch(poGRider,
                    lsSQL,
                    fsValue,
                    lsHeader,
                    lsColName,
                    lsColCrit,
                    fbSearch ? 1 : 5);
        }

        if (loJSON != null) {
            psStockIDx = (String) loJSON.get("sStockIDx");
            psBarCodex = (String) loJSON.get("sBarCodex");
            psDescript = (String) loJSON.get("sDescript");
        } else {
            psStockIDx = "";
            psBarCodex = "";
            psDescript = "";

            psWarnMsg = "No record found/selected. Please verify your entry.";
            psErrMsgx = "";
        }

        if (openInvRecord(psStockIDx)) {
            poData = openRecord(psStockIDx);
            if (poData == null) {
                return NewRecord();
            } else {
                return true;
            }
        } else {
            pnEditMode = EditMode.UNKNOWN;
            return false;
        }
    }

    public boolean SearchInventory(String fsValue, boolean fbSearch, boolean fbByCode) {
        String lsHeader = "Barcode»Description»Brand»Unit»Model»Inv. Type»Stock ID";
        String lsColName = "sBarCodex»sDescript»xBrandNme»sMeasurNm»xModelNme»xInvTypNm»sStockIDx";
        String lsColCrit = "a.sBarCodex»a.sDescript»b.sDescript»e.sMeasurNm»c.sDescript»d.sDescript»a.sStockIDx";
        String lsSQL = getSQ_Inventory();

//        String lsSQL = MiscUtil.addCondition(getSQ_Inventory(), "a.cRecdStat = " + SQLUtil.toSQL(RecordStatus.ACTIVE));
        JSONObject loJSON;

        if (fbByCode) {
            lsSQL = MiscUtil.addCondition(lsSQL, "a.sStockIDx = " + SQLUtil.toSQL(fsValue));

            ResultSet loRS = poGRider.executeQuery(lsSQL);

            loJSON = CommonUtils.loadJSON(loRS);
        } else {
            loJSON = showFXDialog.jsonSearch(poGRider,
                    lsSQL,
                    fsValue,
                    lsHeader,
                    lsColName,
                    lsColCrit,
                    fbSearch ? 1 : 5);
        }

        if (loJSON != null) {
            psStockIDx = (String) loJSON.get("sStockIDx");
            psBarCodex = (String) loJSON.get("sBarCodex");
            psDescript = (String) loJSON.get("sDescript");
        } else {
            psStockIDx = "";
            psBarCodex = "";
            psDescript = "";
        }

        if (openInvRecord(psStockIDx)) {
            poData = openRecord(psStockIDx);
            if (poData == null) {
                return NewRecord();
            } else {
                return true;
            }
        } else {
            pnEditMode = EditMode.UNKNOWN;
            return false;
        }
    }

    public ResultSet GetHistory() {
        String lsSQL = "SELECT"
                + "  a.dTransact"
                + ", b.sDescript"
                + ", a.sSourceNo"
                + ", a.nQtyInxxx"
                + ", a.nQtyOutxx"
                + ", a.nQtyOnHnd"
                + ", a.sSourceCd"
                + ", CASE a.sSourceCd "
                + " WHEN 'Dlvr' THEN d.sBranchNm "
                + " WHEN 'AcDl' THEN f.sBranchNm "
                + " ELSE e.sBranchNm "
                + " END sBranchNm "
                + " FROM Inv_Ledger a"
                + " LEFT JOIN xxxSource_Transaction b"
                + " ON a.sSourceCd = b.sSourceCd  "
                + " LEFT JOIN Inv_Transfer_Master c"
                + " ON a.sSourceNo = c.sTransNox AND a.sSourceCd = 'Dlvr'"
                + " LEFT JOIN Branch d"
                + " ON c.sDestinat = d.sBranchCd"
                + " LEFT JOIN Inv_Transfer_Master g "
                + " ON a.sSourceNo = g.sTransNox AND a.sSourceCd = 'AcDl' "
                + " LEFT JOIN Branch f"
                + " ON LEFT(g.sTransNox,4) = f.sBranchCd"
                + " LEFT JOIN Branch e"
                + " ON LEFT(a.sSourceNo,4) = e.sBranchCd"
                + " WHERE a.sBranchCd = " + SQLUtil.toSQL(poData.getBranchCd())
                + " AND a.sStockIDx = " + SQLUtil.toSQL(poData.getStockIDx())
                + " ORDER BY a.dTransact ASC, a.nLedgerNo ASC";
        System.out.println(lsSQL);
        ResultSet loRS = poGRider.executeQuery(lsSQL);
        System.err.println(lsSQL);

        return loRS;
    }

    private UnitInvSerial openSerial(String fsSerialID) {
        UnitInvSerial loData = null;

        Connection loConn = null;
        loConn = setConnection();

        String lsSQL = MiscUtil.addCondition(getSQ_Serial(), "sSerialID = " + SQLUtil.toSQL(fsSerialID));
        ResultSet loRS = poGRider.executeQuery(lsSQL);

        try {
            if (!loRS.next()) {
                setMessage("No Record Found");
            } else {
                loData = new UnitInvSerial();
                for (int lnCol = 1; lnCol <= loRS.getMetaData().getColumnCount(); lnCol++) {
                    loData.setValue(lnCol, loRS.getObject(lnCol));
                }
            }
        } catch (SQLException ex) {
            setErrMsg(ex.getMessage());
            return null;
        } finally {
            MiscUtil.close(loRS);
            if (!pbWithParent) {
                MiscUtil.close(loConn);
            }
        }

        pnEditMode = EditMode.READY;
        return loData;
    }

    private UnitInvMaster openRecord(String fsValue) {
        UnitInvMaster loData = null;

        Connection loConn = null;
        loConn = setConnection();

        String lsSQL = MiscUtil.addCondition(getSQ_Master(),
                "sStockIDx = " + SQLUtil.toSQL(fsValue)
                + " AND sBranchCd = " + SQLUtil.toSQL(psBranchCd));

        ResultSet loRS = poGRider.executeQuery(lsSQL);

        try {
            if (!loRS.next()) {
                setMessage("No Record Found");
            } else {
                loData = new UnitInvMaster();
                for (int lnCol = 1; lnCol <= loRS.getMetaData().getColumnCount(); lnCol++) {
                    loData.setValue(lnCol, loRS.getObject(lnCol));
                }
            }
        } catch (SQLException ex) {
            setErrMsg(ex.getMessage());
            return null;
        } finally {
            MiscUtil.close(loRS);
            if (!pbWithParent) {
                MiscUtil.close(loConn);
            }
        }

        pnEditMode = EditMode.READY;
        return loData;
    }

    private boolean openInvRecord(String fsStockIDx) {
        if (fsStockIDx.equals("")) {
            return false;
        }

        return poInventory.OpenRecord(fsStockIDx);
    }

    public boolean NewRecord() {
        Connection loConn = null;
        loConn = setConnection();

        poData = new UnitInvMaster();
        poData.setAcquired(poGRider.getServerDate());
        poData.setBegInvxx(poGRider.getServerDate());

        poData.setStockIDx(psStockIDx);
        poData.setBranchCd(psBranchCd);

        pnEditMode = EditMode.ADDNEW;
        return true;
    }

    public boolean UpdateRecord() {
        if (pnEditMode != EditMode.READY) {
            return false;
        } else {
            pnEditMode = EditMode.UPDATE;
            return true;
        }
    }

    public boolean SaveRecord() {
        String lsSQL = "";
        UnitInvMaster loOldEnt = null;
        UnitInvMaster loNewEnt = null;
        UnitInvMaster loResult = null;

        loNewEnt = (UnitInvMaster) poData;

        // Test if entry is ok
        if (loNewEnt.getStockIDx() == null || loNewEnt.getStockIDx().isEmpty()) {
            setMessage("Invalid stock id detected.");
            return false;
        }

        if (loNewEnt.getBranchCd() == null || loNewEnt.getBranchCd().isEmpty()) {
            setMessage("Invalid branch detected.");
            return false;
        }

        loNewEnt.setModified(psUserIDxx);
        loNewEnt.setDateModified(poGRider.getServerDate());

        // Generate the SQL Statement
        if (pnEditMode == EditMode.ADDNEW) {
            Connection loConn = null;
            loConn = setConnection();

            if (!pbWithParent) {
                MiscUtil.close(loConn);
            }

            //Generate the SQL Statement
            lsSQL = MiscUtil.makeSQL((GEntity) loNewEnt);
        } else {
            //Load previous transaction
            loOldEnt = openRecord(psStockIDx);

            //Generate the Update Statement
            lsSQL = MiscUtil.makeSQL((GEntity) loNewEnt,
                    (GEntity) loOldEnt,
                    "sStockIDx = " + SQLUtil.toSQL(loNewEnt.getValue(1))
                    + " AND sBranchCd = " + SQLUtil.toSQL(loNewEnt.getValue(2)));
        }

        //No changes have been made
        if (lsSQL.equals("")) {
            setMessage("Record is not updated");
            return false;
        }

        if (!pbWithParent) {
            poGRider.beginTrans();
        }

        boolean lbUpdate = false;

        if (poGRider.executeQuery(lsSQL, loNewEnt.getTable(), "", "") == 0) {
            if (!poGRider.getErrMsg().isEmpty()) {
                setErrMsg(poGRider.getErrMsg());
            } else {
                setMessage("No record updated");
            }
        } else {
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

    public Object getInventory(String fsCol) {
        return getInventory(poDataInv.getColumn(fsCol));
    }

    public Object getInventory(int fnCol) {
        return poInventory.getMaster(fnCol);
    }

    public Object getSerial(String fsCol) {
        return poSerial.getValue(fsCol);
    }

    public Object getSerial(int fnCol) {
        return getSerial(poSerial.getColumn(fnCol));
    }

    public String SearchInventory(int fnCol, String fsValue, boolean fbByCode) {
        String lsHeader;
        String lsColName;
        String lsColCrit;
        String lsSQL;
        JSONObject loJSON;

        if (fsValue.equals("") && fbByCode) {
            return "";
        }

        switch (fnCol) {
            case 6: //sCategCd1
                XMCategory loCategory = new XMCategory(poGRider, psBranchCd, true);

                loJSON = loCategory.searchCategory(fsValue, fbByCode);

                if (loJSON == null) {
                    return "";
                } else {
                    return (String) loJSON.get("sDescript");
                }
            case 7: //sCategCd2
                XMCategoryLevel2 loCategory2 = new XMCategoryLevel2(poGRider, psBranchCd, true);

                loJSON = loCategory2.searchCategory(fsValue, fbByCode);

                if (loJSON == null) {
                    return "";
                } else {
                    return (String) loJSON.get("sDescript");
                }
            case 8: //sCategCd3
                XMCategoryLevel3 loCategory3 = new XMCategoryLevel3(poGRider, psBranchCd, true);

                loJSON = loCategory3.searchCategory(fsValue, fbByCode);

                if (loJSON == null) {
                    return "";
                } else {
                    return (String) loJSON.get("sDescript");
                }
            case 9: //sCategCd4
                XMCategoryLevel4 loCategory4 = new XMCategoryLevel4(poGRider, psBranchCd, true);

                loJSON = loCategory4.searchCategory(fsValue, fbByCode);

                if (loJSON == null) {
                    return "";
                } else {
                    return (String) loJSON.get("sDescript");
                }
            case 10: //sBrandCde
                XMBrand loBrand = new XMBrand(poGRider, psBranchCd, true);

                loJSON = loBrand.searchBrand(fsValue, fbByCode);

                if (loJSON == null) {
                    return "";
                } else {
                    return (String) loJSON.get("sDescript");
                }
            case 11: //sModelCde
                XMModel loModel = new XMModel(poGRider, psBranchCd, false);

                loJSON = loModel.searchModel(fsValue, fbByCode);

                if (loJSON == null) {
                    return "";
                } else {
                    return (String) loJSON.get("sModelNme");
                }
            case 12: //sColorCde
                XMColor loColor = new XMColor(poGRider, psBranchCd, false);

                loJSON = loColor.searchColor(fsValue, fbByCode);

                if (loJSON == null) {
                    return "";
                } else {
                    return (String) loJSON.get("sDescript");
                }
            case 13: //sInvTypCd
                XMInventoryType loInvType = new XMInventoryType(poGRider, psBranchCd, false);

                loJSON = loInvType.searchInvType(fsValue, fbByCode);

                if (loJSON == null) {
                    return "";
                } else {
                    return (String) loJSON.get("sDescript");
                }
            case 29: //sMeasurID
                XMMeasure loMeasure = new XMMeasure(poGRider, psBranchCd, false);

                loJSON = loMeasure.searchMeasure(fsValue, fbByCode);

                if (loJSON == null) {
                    return "";
                } else {
                    return (String) loJSON.get("sMeasurNm");
                }
            default:
                return "";
        }
    }

    public String SearchInventory(String fsCol, String fsValue, boolean fbByCode) {
        return SearchInventory(poDataInv.getColumn(fsCol), fsValue, fbByCode);
    }

    public String SearchMaster(int fnCol, String fsValue, boolean fbByCode) {
        String lsHeader = "";
        String lsColName = "";
        String lsColCrit = "";
        String lsSQL = "";
        JSONObject loJSON;

        if (fsValue.equals("") && fbByCode) {
            return "";
        }

        switch (fnCol) {
            case 3: //sLocatnCd
                XMInventoryLocation loLocation = new XMInventoryLocation(poGRider, psBranchCd, false);

                loJSON = loLocation.searchLocation(fsValue, fbByCode);

                if (loJSON != null) {
                    setMaster(fnCol, (String) loJSON.get("sLocatnCd"));
                    return (String) loJSON.get("sDescript");
                } else {
                    setMaster(fnCol, "");
                    return "";
                }
            default:
                return "";
        }
    }

    public String SearchMaster(String fsCol, String fsValue, boolean fbByCode) {
        return SearchMaster(poData.getColumn(fsCol), fsValue, fbByCode);
    }

    public void setMaster(int fnCol, Object foData) {
        if (pnEditMode != EditMode.UNKNOWN) {
            // Don't allow specific fields to assign values
            if (!(fnCol == poData.getColumn("sStockIDx")
                    || fnCol == poData.getColumn("cRecdStat")
                    || fnCol == poData.getColumn("sModified")
                    || fnCol == poData.getColumn("dModified"))) {

                if (fnCol == poData.getColumn("nBegQtyxx")
                        || fnCol == poData.getColumn("nQtyOnHnd")
                        || fnCol == poData.getColumn("nMinLevel")
                        || fnCol == poData.getColumn("nMaxLevel")
                        || fnCol == poData.getColumn("nAvgMonSl")
                        || fnCol == poData.getColumn("nBackOrdr")
                        || fnCol == poData.getColumn("nResvOrdr")
                        || fnCol == poData.getColumn("nFloatQty")) {
                    if (foData instanceof Number) {
                        poData.setValue(fnCol, foData);
                    } else {
                        poData.setValue(fnCol, 0);
                    }
                } else if (fnCol == poData.getColumn("nFloatQty")) {
                    if (foData instanceof Number) {
                        poData.setValue(fnCol, foData);
                    } else {
                        poData.setValue(fnCol, 0.00);
                    }
                } else {
                    System.out.println("fnCol = " + fnCol + " foData = " + foData);
                    poData.setValue(fnCol, foData);
                }
            }
        }
    }

    public void setMaster(String fsCol, Object foData) {
        System.out.println(poData.getColumn(fsCol));
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

    public void setGRider(GRider foGRider) {
        this.poGRider = foGRider;
        this.psUserIDxx = foGRider.getUserID();

        if (psBranchCd.isEmpty()) {
            psBranchCd = foGRider.getBranchCode();
        }
    }

    public void setUserID(String fsUserID) {
        this.psUserIDxx = fsUserID;
    }

    public int getEditMode() {
        return pnEditMode;
    }

    public void printColumnsInvMaster() {
        poData.list();
    }

    public void printColumnsInventory() {
        poDataInv.list();
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

    public boolean recalculatex() throws SQLException {
        String lsSQL = "SELECT "
                + "a.sStockIDx,"
                + "a.dBegInvxx,"
                + "a.nBegQtyxx"
                + " FROM Inv_Master a"
                + ", Inventory b"
                + " WHERE a.sStockIDx = b.sStockIDx"
                + " AND a.sBranchCd = " + SQLUtil.toSQL(psBranchCd)
                + " AND a.cRecdStat = '1'"
                + " AND b.cRecdStat = '1'"
                + " AND a.sStockIDx IS NOT NULL AND a.sStockIDx <> ''";

        if (!System.getProperty("store.inventory.type").isEmpty()) {
//                lsSQL = MiscUtil.addCondition(lsSQL, "b.sInvTypCd IN " + CommonUtils.getParameter(System.getProperty("store.inventory.type")));
        }

        ResultSet loRS = poGRider.executeQuery(lsSQL);

        int lnMax = (int) MiscUtil.RecordCount(loRS);
        if (lnMax <= 0) {
            setMessage("No record to recalculate.");
            return false;
        }

        loRS.beforeFirst();
        int lnRow = 1;
        while (loRS.next()) {
            if (!recalculate(loRS.getString("sStockIDx"), loRS.getDate("dBegInvxx"), loRS.getDouble("nBegQtyxx"))) {
                setMessage("Unable to recalculate " + poData.getStockIDx() + ".");
                return false;
            }
            lnRow += 1;

            System.out.println(lnRow);
        }
        return true;
    }

    //mac 2024.12.06
    /**
     * recalculate()
     *
     * @return true or false
     * @throws SQLException
     */
    public boolean recalculate() throws SQLException {
        String lsSQL = "SELECT "
                + "a.sStockIDx"
                + " FROM Inv_Master a"
                + ", Inventory b"
                + " WHERE a.sStockIDx = b.sStockIDx"
                + " AND a.sBranchCd = " + SQLUtil.toSQL(psBranchCd)
                + " AND a.cRecdStat = '1'"
                + " AND b.cRecdStat = '1'";

        ResultSet loRS = poGRider.executeQuery(lsSQL);

        if (MiscUtil.RecordCount(loRS) <= 0) {
            setMessage("No record to recalculate.");
            return false;
        }

        poGRider.beginTrans();

        while (loRS.next()) {
            if (!recalculate(loRS.getString("sStockIDx"), true)) {
                poGRider.rollbackTrans();
                return false;
            }
        }

        poGRider.commitTrans();

        return true;
    }

    //mac 2024.12.06
    public boolean reAlignOnHand(String fsStockIDx, Date ldTransact) throws SQLException {
        String lsSQL;

        double lnQOH;

        int lnCtr = 0;
        int lnLedgerNo;

        Date ldBegInv = poGRider.getServerDate();

        //find inventory
        lsSQL = "SELECT a.sStockIDx, a.dBegInvxx, a.nBegQtyxx"
                + " FROM Inv_Master a"
                + ", Inventory b"
                + " WHERE a.sStockIDx = b.sStockIDx"
                + " AND a.sStockIDx = " + SQLUtil.toSQL(fsStockIDx)
                + " AND a.sBranchCd = " + SQLUtil.toSQL(psBranchCd);

        ResultSet loRS = poGRider.executeQuery(lsSQL);

        if (MiscUtil.RecordCount(loRS) <= 0) {
            setMessage("No inventory found for this branch.");
            return false;
        }

        //get the starting on hand from the ledger of the transaction date
        lsSQL = "SELECT * FROM Inv_Ledger"
                + " WHERE sStockIDx = " + SQLUtil.toSQL(fsStockIDx)
                + " AND sBranchCd = " + SQLUtil.toSQL(psBranchCd)
                + " AND dTransact < " + SQLUtil.toSQL(SQLUtil.dateFormat(ldTransact, SQLUtil.FORMAT_SHORT_DATE))
                + " ORDER BY nLedgerNo DESC"
                + " LIMIT 1";

        loRS = poGRider.executeQuery(lsSQL);

        //beginning quantity and ledger no
        if (!loRS.next()) {
            lnQOH = 0;
            lnLedgerNo = 0;
        } else {
            lnQOH = loRS.getDouble("nQtyOnHnd");
            lnLedgerNo = loRS.getInt("nLedgerNo");
        }

        //load the ledger from the date of transaction
        lsSQL = "SELECT * FROM Inv_Ledger"
                + " WHERE sStockIDx = " + SQLUtil.toSQL(fsStockIDx)
                + " AND sBranchCd = " + SQLUtil.toSQL(psBranchCd)
                + " AND dTransact >= " + SQLUtil.toSQL(SQLUtil.dateFormat(ldTransact, SQLUtil.FORMAT_SHORT_DATE))
                + " ORDER BY dTransact, nLedgerNo";

        loRS = poGRider.executeQuery(lsSQL);

        if (MiscUtil.RecordCount(loRS) <= 0) {
            return true;
        }

        if (!pbWithParent) {
            poGRider.beginTrans();
        }

        //recalculate the ledger
        while (loRS.next()) {
            lnLedgerNo++;
            lnQOH += (loRS.getDouble("nQtyInxxx") - loRS.getDouble("nQtyOutxx"));

            lsSQL = "UPDATE Inv_Ledger SET"
                    + "  nLedgerNo = " + lnLedgerNo
                    + ", nQtyOnHnd = " + lnQOH
                    + ", sModified = " + SQLUtil.toSQL(poGRider.getUserID())
                    + ", dModified = " + SQLUtil.toSQL(poGRider.getServerDate())
                    + " WHERE sStockIDx = " + SQLUtil.toSQL(fsStockIDx)
                    + " AND sBranchCd = " + SQLUtil.toSQL(psBranchCd)
                    + " AND nLedgerNo = " + loRS.getInt("nLedgerNo")
                    + " AND sSourceCd = " + SQLUtil.toSQL(loRS.getString("sSourceCd"))
                    + " AND sSourceNo = " + SQLUtil.toSQL(loRS.getString("sSourceNo"));

            if (poGRider.executeQuery(lsSQL, "Inv_Ledger", psBranchCd, "") != 1) {
                if (!pbWithParent) {
                    poGRider.rollbackTrans();
                }
                setMessage("Unable to execute ledger update.");
                return false;
            }

            ldBegInv = loRS.getDate("dTransact");

            lnCtr++;
        }

        lsSQL = "UPDATE Inv_Master SET"
                + "  nQtyOnHnd = " + lnQOH
                + ", nLedgerNo = " + lnLedgerNo
                + ", dLastTran = " + SQLUtil.toSQL(SQLUtil.dateFormat(ldBegInv, SQLUtil.FORMAT_SHORT_DATE))
                + ", sModified = " + SQLUtil.toSQL(poGRider.getUserID())
                + ", dModified = " + SQLUtil.toSQL(poGRider.getServerDate())
                + " WHERE sStockIDx = " + SQLUtil.toSQL(fsStockIDx)
                + " AND sBranchCd = " + SQLUtil.toSQL(psBranchCd);

        if (poGRider.executeQuery(lsSQL, "Inv_Master", psBranchCd, "") != 1) {
            if (!pbWithParent) {
                poGRider.rollbackTrans();
            }
            setMessage("Unable to execute inventory update.");
            return false;
        }

        if (!pbWithParent) {
            poGRider.commitTrans();
        }

        return true;
    }

    //mac 2024.11.27
    /**
     * recalculate(String fsStockIDx, String fsDateFrom, String fsDateThru)
     *
     * @param fsStockIDx - Stock ID
     * @param fbWtParent - Is the procedure has parent procedure?
     * @return true or false
     * @throws SQLException
     */
    public boolean recalculate(String fsStockIDx, boolean fbWtParent) throws SQLException {
        String lsSQL;

        double lnQOH = 0.00;
        Date ldBegInv = poGRider.getServerDate();
        Date ldAcquired;

        //find inventory
        lsSQL = "SELECT a.sStockIDx, a.dBegInvxx, a.dAcquired, a.nBegQtyxx"
                + " FROM Inv_Master a"
                + ", Inventory b"
                + " WHERE a.sStockIDx = b.sStockIDx"
                + " AND a.sStockIDx = " + SQLUtil.toSQL(fsStockIDx)
                + " AND a.sBranchCd = " + SQLUtil.toSQL(psBranchCd);

        ResultSet loRS = poGRider.executeQuery(lsSQL);

        if (MiscUtil.RecordCount(loRS) <= 0) {
            setMessage("No inventory found for this branch.");
            return false;
        }
        
        //get beginning inventory date and quantity
        if (loRS.next()) {
            ldAcquired = loRS.getDate("dAcquired");
            ldBegInv = loRS.getDate("dBegInvxx");
            lnQOH = loRS.getDouble("nBegQtyxx");
            
            if (ldBegInv == null){
                lsSQL = "SELECT * FROM Inv_Ledger" +
                        " WHERE sStockIDx = " + SQLUtil.toSQL(fsStockIDx) +
                            " AND sBranchCd = " + SQLUtil.toSQL(psBranchCd) +
                        " ORDER BY dTransact, nLedgerNo LIMIT 1";
                
                loRS = poGRider.executeQuery(lsSQL);
                
                if (loRS.next()){                   
                    ldBegInv = loRS.getDate("dTransact");
                    lnQOH = loRS.getDouble("nQtyInxxx");
                    
                    if (ldBegInv.after(ldAcquired)){
                        ldBegInv = ldAcquired;
                        lnQOH = 0;
                    }
                    
                    lsSQL = "UPDATE Inv_Master SET" +
                                "  dBegInvxx = " + SQLUtil.toSQL(ldBegInv) +
                                ", nBegQtyxx = " + lnQOH +
                            " WHERE sStockIDx = " + SQLUtil.toSQL(fsStockIDx) +
                            " AND sBranchCd = " + SQLUtil.toSQL(psBranchCd);
                    
                    if (poGRider.executeQuery(lsSQL, "Inv_Master", psBranchCd, "") != 1) {
                        setMessage("Unable to execute ledger update.");
                        if (!fbWtParent) {
                            poGRider.rollbackTrans();
                        }
                        return false;
                    }
                }
            }
        }

        //load the ledger after the beginning inventory date
        lsSQL = "SELECT * FROM Inv_Ledger"
                + " WHERE sStockIDx = " + SQLUtil.toSQL(fsStockIDx)
                    + " AND sBranchCd = " + SQLUtil.toSQL(psBranchCd)
                    + " AND dTransact > " + SQLUtil.toSQL(SQLUtil.dateFormat(ldBegInv, SQLUtil.FORMAT_SHORT_DATE))
                + " ORDER BY dTransact, nLedgerNo";

        loRS = poGRider.executeQuery(lsSQL);

        if (MiscUtil.RecordCount(loRS) <= 0) {
            return true;
        }

        int lnLedgerNo = 0;

        if (!fbWtParent) {
            poGRider.beginTrans();
        }

        //recalculate the ledger
        while (loRS.next()) {
            lnLedgerNo++;
            lnQOH += (loRS.getDouble("nQtyInxxx") - loRS.getDouble("nQtyOutxx"));

            lsSQL = "UPDATE Inv_Ledger SET" +
                        "  nLedgerNo = " + lnLedgerNo +
                        ", nQtyOnHnd = " + lnQOH +
                        ", sModified = " + SQLUtil.toSQL(poGRider.getUserID()) +
                        ", dModified = " + SQLUtil.toSQL(poGRider.getServerDate()) +
                    " WHERE sStockIDx = " + SQLUtil.toSQL(fsStockIDx) +
                        " AND sBranchCd = " + SQLUtil.toSQL(psBranchCd) +
                        " AND nLedgerNo = " + loRS.getInt("nLedgerNo") +
                        " AND sSourceCd = " + SQLUtil.toSQL(loRS.getString("sSourceCd")) +
                        " AND sSourceNo = " + SQLUtil.toSQL(loRS.getString("sSourceNo"));
            
            if (poGRider.executeQuery(lsSQL, "Inv_Ledger", psBranchCd, "") != 1) {
                setMessage("Unable to execute ledger update.");
                if (!fbWtParent) {
                    poGRider.rollbackTrans();
                }
                return false;
            }

            ldBegInv = loRS.getDate("dTransact");
        }

        lsSQL = "UPDATE Inv_Master SET"
                + "  nQtyOnHnd = " + lnQOH
                + ", nLedgerNo = " + lnLedgerNo
                + ", dLastTran = " + SQLUtil.toSQL(SQLUtil.dateFormat(ldBegInv, SQLUtil.FORMAT_SHORT_DATE))
                + ", sModified = " + SQLUtil.toSQL(poGRider.getUserID())
                + ", dModified = " + SQLUtil.toSQL(poGRider.getServerDate())
                + " WHERE sStockIDx = " + SQLUtil.toSQL(fsStockIDx)
                + " AND sBranchCd = " + SQLUtil.toSQL(psBranchCd);

        if (poGRider.executeQuery(lsSQL, "Inv_Master", psBranchCd, "") != 1) {
            setMessage("Unable to execute inventory update.");
            if (!fbWtParent) {
                poGRider.rollbackTrans();
            }
            return false;
        }

        if (!fbWtParent) {
            poGRider.commitTrans();
        }

        return true;
    }

    public boolean recalculatex(String fsStockIDx) throws SQLException {
        if (fsStockIDx == null) {
            String lsSQL = "SELECT a.sStockIDx"
                    + " FROM Inv_Master a"
                    + ", Inventory b"
                    + " WHERE a.sStockIDx = b.sStockIDx"
                    + " AND a.sBranchCd = " + SQLUtil.toSQL(psBranchCd)
                    + " AND a.cRecdStat = '1'"
                    + " AND b.cRecdStat = '1'";

            if (!System.getProperty("store.inventory.type").isEmpty()) {
//                lsSQL = MiscUtil.addCondition(lsSQL, "b.sInvTypCd IN " + CommonUtils.getParameter(System.getProperty("store.inventory.type")));
            }

            ResultSet loRS = poGRider.executeQuery(lsSQL);

            int lnMax = (int) MiscUtil.RecordCount(loRS);
            if (lnMax <= 0) {
                setMessage("No record to recalculate.");
                return false;
            }

            loRS.beforeFirst();
            int lnRow = 1;
            while (loRS.next()) {
                if (!SearchInventory(loRS.getString("sStockIDx"), false, true)) {
                    setMessage("No record found.");
                    return false;
                }

                if (!recalculate(loRS.getString("sStockIDx"), poData.getBegInvxx(), poData.getBegQtyxx().doubleValue())) {
                    setMessage("Unable to recalculate " + poData.getStockIDx() + ".");
                    return false;
                }

                lnRow += 1;

                System.out.println(lnRow);
            }

            return true;
        } else {
            if (pnEditMode != EditMode.READY) {
                if (!SearchInventory(fsStockIDx, false, true)) {
                    return false;
                }
            } else {
                if (!poData.getStockIDx().equalsIgnoreCase(fsStockIDx)) {
                    if (!SearchInventory(fsStockIDx, false, true)) {
                        return false;
                    }
                }
            }

            return recalculate(fsStockIDx, poData.getBegInvxx(), poData.getBegQtyxx().doubleValue());
        }
    }

    public boolean recalculate(String fsStockIDx, Date fdBegInvxx, double fnBegQtyxx) throws SQLException {
        String lsSQL = "";
        ResultSet loRSMaster;

        //check if beginning inventory date is less than beginning inventory lock date
        if (poData.getBegInvxx() != null) {
            if (poData.getBegInvxx().before(fdBegInvxx)) {
                psWarnMsg = "Beginning date is less than the current beginning date!";
            }
        }

        poGRider.beginTrans();
        ResultSet loRSLedger;

//        temporary disable:
//        *delete ledger
//        *insert ledger sa history
//        if (fdBegInvxx != null) {
//            //check if there are transaction before the beginning inventory date
//            //transfer to history ledger
//            lsSQL = "SELECT sSourceNo"
//                    + " FROM Inv_Ledger"
//                    + " WHERE sStockIDx = " + SQLUtil.toSQL(fsStockIDx)
//                    + " AND sBranchCD = " + SQLUtil.toSQL(poGRider.getBranchCode())
//                    + " AND dTransact <= " + SQLUtil.toSQL(fdBegInvxx)
//                    + " ORDER BY dTransact, nLedgerNo";
//
//            loRSLedger = poGRider.executeQuery(lsSQL);
//            if (loRSLedger.next()) {
//                //Insert into history ledger
//                lsSQL = "INSERT INTO Inv_Ledger_Hist"
//                        + " SELECT * FROM Inv_Ledger"
//                        + " WHERE sStockIDx = " + SQLUtil.toSQL(fsStockIDx)
//                        + " AND sBranchCD = " + SQLUtil.toSQL(poGRider.getBranchCode())
//                        + " AND dTransact <= " + SQLUtil.toSQL(fdBegInvxx)
//                        + " ORDER BY dTransact, nLedgerNo";
//                System.out.println(lsSQL);
//                poGRider.executeQuery(lsSQL, "Inv_Ledger_Hist", psBranchCd, "");
//
//                //delete transaction from inventory ledger
//                lsSQL = "DELETE FROM Inv_Ledger"
//                        + " WHERE sStockIDx = " + SQLUtil.toSQL(fsStockIDx)
//                        + " AND sBranchCD = " + SQLUtil.toSQL(poGRider.getBranchCode())
//                        + " AND dTransact <= " + SQLUtil.toSQL(fdBegInvxx);
//                System.out.println(lsSQL);
//                poGRider.executeQuery(lsSQL, "Inv_Ledger", psBranchCd, "");
//
//            }
//
//            //check if history ledger has a transaction before inventory date
//            //if there are transactions then transfer it to the transaction ledger
//            lsSQL = "SELECT sSourceNo"
//                    + " FROM Inv_Ledger_Hist"
//                    + " WHERE sStockIDx = " + SQLUtil.toSQL(fsStockIDx)
//                    + " AND sBranchCD = " + SQLUtil.toSQL(poGRider.getBranchCode())
//                    + " AND dTransact > " + SQLUtil.toSQL(fdBegInvxx);
//
//            loRSLedger = poGRider.executeQuery(lsSQL);
//            if (loRSLedger.next()) {
//                //Insert into transaction ledger
//                lsSQL = "INSERT INTO Inv_Ledger"
//                        + " SELECT * FROM Inv_Ledger_Hist"
//                        + " WHERE sStockIDx = " + SQLUtil.toSQL(fsStockIDx)
//                        + " AND sBranchCD = " + SQLUtil.toSQL(poGRider.getBranchCode())
//                        + " AND dTransact > " + SQLUtil.toSQL(fdBegInvxx);
//                System.out.println(lsSQL);
//                poGRider.executeQuery(lsSQL, "Inv_Ledger", psBranchCd, "");
//
//                //delete transaction from inventory ledger
//                lsSQL = "DELETE FROM Inv_Ledger_Hist"
//                        + " WHERE sStockIDx = " + SQLUtil.toSQL(fsStockIDx)
//                        + " AND sBranchCD = " + SQLUtil.toSQL(poGRider.getBranchCode())
//                        + " AND dTransact > " + SQLUtil.toSQL(fdBegInvxx);
//                System.out.println(lsSQL);
//                poGRider.executeQuery(lsSQL, "Inv_Ledger_Hist", psBranchCd, "");
//            }
//
//        }
        double lnQtyOnHnd = fnBegQtyxx;

        int lnLedgerNo = 0;
        lsSQL = "SELECT *"
                + " FROM Inv_Ledger"
                + " WHERE sStockIDx = " + SQLUtil.toSQL(fsStockIDx)
                + " AND sBranchCD = " + SQLUtil.toSQL(poGRider.getBranchCode())
                + " ORDER BY dTransact, nLedgerNo";
        loRSLedger = poGRider.executeQuery(lsSQL);
        System.out.println(lsSQL);

        while (loRSLedger.next()) {
            StringBuilder loSQL = new StringBuilder();

            lnQtyOnHnd += (loRSLedger.getFloat("nQtyInxxx") - loRSLedger.getFloat("nQtyOutxx"));
            lnLedgerNo++;

            if (lnLedgerNo != loRSLedger.getInt("nLedgerNo")) {
                loSQL.append(", ").append("nLedgerNo = ").append(lnLedgerNo);
            }

            if (Double.compare(loRSLedger.getDouble("nQtyOnHnd"), lnQtyOnHnd) != 0) {
                loSQL.append(", ").append("nQtyOnHnd = ").append(lnQtyOnHnd);
            }

            if (loSQL.length() > 0) {

                System.out.println("\nlnLedgerNo = " + lnLedgerNo + " nLedgerNo = " + loRSLedger.getInt("nLedgerNo"));
                lsSQL = "UPDATE Inv_Ledger"
                        + " SET " + loSQL.toString().substring(2)
                        + ", dModified = " + SQLUtil.toSQL(poGRider.getServerDate())
                        + " WHERE sStockIDx = " + SQLUtil.toSQL(fsStockIDx)
                        + " AND sBranchCD = " + SQLUtil.toSQL(poGRider.getBranchCode())
                        + " AND sSourceCd = " + SQLUtil.toSQL(loRSLedger.getString("sSourceCd"))
                        + " AND sSourceNo = " + SQLUtil.toSQL(loRSLedger.getString("sSourceNo"));
                System.out.println(lsSQL);
                poGRider.executeQuery(lsSQL, "Inv_Ledger", psBranchCd, "");
            }
        }

        StringBuilder loSQL = new StringBuilder();

        if (lnLedgerNo != poData.getLedgerNo()) {
            loSQL.append(", ").append("nLedgerNo = ").append(lnLedgerNo);
        }

        if (Double.compare(poData.getQtyOnHnd().doubleValue(), lnQtyOnHnd) != 0) {
            loSQL.append(", ").append("nQtyOnHnd = ").append(lnQtyOnHnd);
        }

        if (poData.getBegInvxx() == null) {
            loSQL.append(", ").append("dBegInvxx = ").append(SQLUtil.toSQL(fdBegInvxx));
        } else if (!poData.getBegInvxx().equals(fdBegInvxx)) {
            loSQL.append(", ").append("dBegInvxx = ").append(SQLUtil.toSQL(fdBegInvxx));
        }

        if (Double.compare(poData.getBegQtyxx().doubleValue(), fnBegQtyxx) != 0) {
            loSQL.append(", ").append("nBegQtyxx = ").append(fnBegQtyxx);
        }

        if (loSQL.length() > 0) {
            lsSQL = "UPDATE Inv_Master"
                    + " SET " + loSQL.toString().substring(2)
                    + " WHERE sStockIDx = " + SQLUtil.toSQL(fsStockIDx)
                    + " AND sBranchCD = " + SQLUtil.toSQL(poGRider.getBranchCode());
            poGRider.executeQuery(lsSQL, "Inv_Master", psBranchCd, "");
        }

        poGRider.commitTrans();
        return true;
    }

    private String getSQ_Inventory() {
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
                + ", e.sMeasurNm"
                + ", a.cWthExprt"
                + " FROM Inventory a"
                + " LEFT JOIN Brand b"
                + " ON a.sBrandCde = b.sBrandCde"
                + " LEFT JOIN Model c"
                + " ON a.sModelCde = c.sModelCde"
                + " LEFT JOIN Inv_Type d"
                + " ON a.sInvTypCd = d.sInvTypCd"
                + " LEFT JOIN Measure e"
                + " ON e.sMeasurID = a.sMeasurID";

        //validate result based on the assigned inventory type.
        if (!System.getProperty("store.inventory.type").isEmpty()) {
            lsSQL = MiscUtil.addCondition(lsSQL, "a.sInvTypCd IN " + CommonUtils.getParameter(System.getProperty("store.inventory.type")));
        }

        return lsSQL;
    }

    private String getSQ_AllStock() {
        String lsSQL = "SELECT"
                + " a.sStockIDx,"
                + " a.sBarCodex xReferNox,"
                + " a.sDescript,"
                + " a.sBriefDsc,"
                + " a.sAltBarCd,"
                + " a.sCategCd1,"
                + " a.sCategCd2,"
                + " a.sCategCd3,"
                + " a.sCategCd4,"
                + " a.sBrandCde,"
                + " a.sModelCde,"
                + " a.sColorCde,"
                + " a.sInvTypCd,"
                + " a.nUnitPrce,"
                + " a.nSelPrice,"
                + " a.nDiscLev1,"
                + " a.nDiscLev2,"
                + " a.nDiscLev3,"
                + " a.nDealrDsc,"
                + " a.cComboInv,"
                + " a.cWthPromo,"
                + " a.cSerialze,"
                + " a.cUnitType,"
                + " a.cInvStatx,"
                + " a.sSupersed,"
                + " a.cRecdStat,"
                + " b.sDescript xBrandNme,"
                + " c.sDescript xModelNme,"
                + " d.sDescript xInvTypNm,"
                + " e.sMeasurNm,"
                + " f.nQtyOnHnd,"
                + " '' sReferNo1,"
                + " '' sSerialID"
                + " FROM Inventory a"
                + " LEFT JOIN Brand b"
                + " ON a.sBrandCde = b.sBrandCde"
                + " LEFT JOIN Model c"
                + " ON a.sModelCde = c.sModelCde"
                + " LEFT JOIN Inv_Type d"
                + " ON a.sInvTypCd = d.sInvTypCd"
                + " LEFT JOIN Measure e"
                + " ON e.sMeasurID = a.sMeasurID,"
                + " Inv_Master f"
                + " WHERE a.sStockIDx = f.sStockIDx"
                + " AND f.sBranchCd = " + SQLUtil.toSQL(psBranchCd)
                + " AND a.cRecdStat = " + SQLUtil.toSQL(RecordStatus.ACTIVE)
                + " AND a.cSerialze = '0'"; //" AND f.nQtyOnHnd > 0"

        //validate result based on the assigned inventory type.
        if (!System.getProperty("store.inventory.type").isEmpty()) {
            lsSQL = lsSQL + " AND a.sInvTypCd IN " + CommonUtils.getParameter(System.getProperty("store.inventory.type"));
        }

        lsSQL = lsSQL + " UNION SELECT"
                + " a.sStockIDx,"
                + " g.sSerial01 xReferNox,"
                + " a.sDescript,"
                + " a.sBriefDsc,"
                + " a.sAltBarCd,"
                + " a.sCategCd1,"
                + " a.sCategCd2,"
                + " a.sCategCd3,"
                + " a.sCategCd4,"
                + " a.sBrandCde,"
                + " a.sModelCde,"
                + " a.sColorCde,"
                + " a.sInvTypCd,"
                + " a.nUnitPrce,"
                + " a.nSelPrice,"
                + " a.nDiscLev1,"
                + " a.nDiscLev2,"
                + " a.nDiscLev3,"
                + " a.nDealrDsc,"
                + " a.cComboInv,"
                + " a.cWthPromo,"
                + " a.cSerialze,"
                + " a.cUnitType,"
                + " a.cInvStatx,"
                + " a.sSupersed,"
                + " a.cRecdStat,"
                + " b.sDescript xBrandNme,"
                + " c.sDescript xModelNme,"
                + " d.sDescript xInvTypNm,"
                + " e.sMeasurNm,"
                + " 1 nQtyOnHnd,"
                + " IFNULL(g.sSerial02, '') xReferNo1,"
                + " g.sSerialID"
                + " FROM Inventory a"
                + " LEFT JOIN Brand b"
                + " ON a.sBrandCde = b.sBrandCde"
                + " LEFT JOIN Model c"
                + " ON a.sModelCde = c.sModelCde"
                + " LEFT JOIN Inv_Type d"
                + " ON a.sInvTypCd = d.sInvTypCd"
                + " LEFT JOIN Measure e"
                + " ON e.sMeasurID = a.sMeasurID,"
                + " Inv_Master f,"
                + " Inv_Serial g"
                + " WHERE a.sStockIDx = f.sStockIDx"
                + " AND f.sStockIDx = g.sStockIDx"
                + " AND a.cSerialze = '1'"
                + " AND g.cLocation = '1'"
                + " AND g.cSoldStat = '0'"
                + " AND f.sBranchCd = " + SQLUtil.toSQL(psBranchCd)
                + " AND a.cRecdStat = " + SQLUtil.toSQL(RecordStatus.ACTIVE); //" AND f.nQtyOnHnd > 0"

        //validate result based on the assigned inventory type.
        if (!System.getProperty("store.inventory.type").isEmpty()) {
            lsSQL = lsSQL + " AND a.sInvTypCd IN " + CommonUtils.getParameter(System.getProperty("store.inventory.type"));
        }

        return lsSQL;
    }

    private String getSQ_SoldStock() {
        String lsSQL = "SELECT"
                + " a.sStockIDx,"
                + " a.sBarCodex xReferNox,"
                + " a.sDescript,"
                + " a.sBriefDsc,"
                + " a.sAltBarCd,"
                + " a.sCategCd1,"
                + " a.sCategCd2,"
                + " a.sCategCd3,"
                + " a.sCategCd4,"
                + " a.sBrandCde,"
                + " a.sModelCde,"
                + " a.sColorCde,"
                + " a.sInvTypCd,"
                + " a.nUnitPrce,"
                + " a.nSelPrice,"
                + " a.nDiscLev1,"
                + " a.nDiscLev2,"
                + " a.nDiscLev3,"
                + " a.nDealrDsc,"
                + " a.cComboInv,"
                + " a.cWthPromo,"
                + " a.cSerialze,"
                + " a.cUnitType,"
                + " a.cInvStatx,"
                + " a.sSupersed,"
                + " a.cRecdStat,"
                + " b.sDescript xBrandNme,"
                + " c.sDescript xModelNme,"
                + " d.sDescript xInvTypNm,"
                + " e.sMeasurNm,"
                + " f.nQtyOnHnd,"
                + " '' sReferNo1,"
                + " '' sSerialID"
                + " FROM Inventory a"
                + " LEFT JOIN Brand b"
                + " ON a.sBrandCde = b.sBrandCde"
                + " LEFT JOIN Model c"
                + " ON a.sModelCde = c.sModelCde"
                + " LEFT JOIN Inv_Type d"
                + " ON a.sInvTypCd = d.sInvTypCd"
                + " LEFT JOIN Measure e"
                + " ON e.sMeasurID = a.sMeasurID,"
                + " Inv_Master f"
                + " WHERE a.sStockIDx = f.sStockIDx"
                + " AND f.sBranchCd = " + SQLUtil.toSQL(psBranchCd)
                + " AND a.cRecdStat = " + SQLUtil.toSQL(RecordStatus.ACTIVE)
                + " AND a.cSerialze = '0'";

        //validate result based on the assigned inventory type.
        if (!System.getProperty("store.inventory.type").isEmpty()) {
            lsSQL = lsSQL + " AND a.sInvTypCd IN " + CommonUtils.getParameter(System.getProperty("store.inventory.type"));
        }

        lsSQL = lsSQL + " UNION SELECT"
                + " a.sStockIDx,"
                + " g.sSerial01 xReferNox,"
                + " a.sDescript,"
                + " a.sBriefDsc,"
                + " a.sAltBarCd,"
                + " a.sCategCd1,"
                + " a.sCategCd2,"
                + " a.sCategCd3,"
                + " a.sCategCd4,"
                + " a.sBrandCde,"
                + " a.sModelCde,"
                + " a.sColorCde,"
                + " a.sInvTypCd,"
                + " a.nUnitPrce,"
                + " a.nSelPrice,"
                + " a.nDiscLev1,"
                + " a.nDiscLev2,"
                + " a.nDiscLev3,"
                + " a.nDealrDsc,"
                + " a.cComboInv,"
                + " a.cWthPromo,"
                + " a.cSerialze,"
                + " a.cUnitType,"
                + " a.cInvStatx,"
                + " a.sSupersed,"
                + " a.cRecdStat,"
                + " b.sDescript xBrandNme,"
                + " c.sDescript xModelNme,"
                + " d.sDescript xInvTypNm,"
                + " e.sMeasurNm,"
                + " 1 nQtyOnHnd,"
                + " IFNULL(g.sSerial02, '') xReferNo1,"
                + " g.sSerialID"
                + " FROM Inventory a"
                + " LEFT JOIN Brand b"
                + " ON a.sBrandCde = b.sBrandCde"
                + " LEFT JOIN Model c"
                + " ON a.sModelCde = c.sModelCde"
                + " LEFT JOIN Inv_Type d"
                + " ON a.sInvTypCd = d.sInvTypCd"
                + " LEFT JOIN Measure e"
                + " ON e.sMeasurID = a.sMeasurID,"
                + " Inv_Master f,"
                + " Inv_Serial g"
                + " WHERE a.sStockIDx = f.sStockIDx"
                + " AND f.sStockIDx = g.sStockIDx"
                + " AND a.cSerialze = '1'"
                + " AND f.sBranchCd = " + SQLUtil.toSQL(psBranchCd)
                + " AND a.cRecdStat = " + SQLUtil.toSQL(RecordStatus.ACTIVE);

        //validate result based on the assigned inventory type.
        if (!System.getProperty("store.inventory.type").isEmpty()) {
            lsSQL = lsSQL + " AND a.sInvTypCd IN " + CommonUtils.getParameter(System.getProperty("store.inventory.type"));
        }

        return lsSQL;
    }

    private String getSQ_Stock() {
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
                + ", e.sMeasurNm"
                + ", f.nQtyOnHnd"
                + " FROM Inventory a"
                + " LEFT JOIN Brand b"
                + " ON a.sBrandCde = b.sBrandCde"
                + " LEFT JOIN Model c"
                + " ON a.sModelCde = c.sModelCde"
                + " LEFT JOIN Inv_Type d"
                + " ON a.sInvTypCd = d.sInvTypCd"
                + " LEFT JOIN Measure e"
                + " ON e.sMeasurID = a.sMeasurID"
                + ", Inv_Master f"
                + " WHERE a.sStockIDx = f.sStockIDx"
                + " AND f.sBranchCd = " + SQLUtil.toSQL(psBranchCd);

        //validate result based on the assigned inventory type.
        if (!System.getProperty("store.inventory.type").isEmpty()) {
            lsSQL = MiscUtil.addCondition(lsSQL, "a.sInvTypCd IN " + CommonUtils.getParameter(System.getProperty("store.inventory.type")));
        }

        return lsSQL;
    }

    private String getSQ_Master() {
        return MiscUtil.makeSelect(new UnitInvMaster());
    }

    private String getSQ_Serial() {
        return MiscUtil.makeSelect(new UnitInvSerial());
    }

    public String getMessage() {
        return psWarnMsg;
    }

    public void setMessage(String fsMessage) {
        this.psWarnMsg = fsMessage;
    }

    public String getErrMsg() {
        return psErrMsgx;
    }

    public void setErrMsg(String fsErrMsg) {
        this.psErrMsgx = fsErrMsg;
    }

    //Member Variables
    private GRider poGRider = null;
    private String psUserIDxx = "";
    private String psBranchCd = "";
    private String psWarnMsg = "";
    private String psErrMsgx = "";
    private boolean pbWithParent = false;
    private int pnEditMode;

    private String psStockIDx = "";
    private String psBarCodex = "";
    private String psDescript = "";
    private String psSerialID = "";
    private String psSerial01 = "";
    private String psSerial02 = "";

    private UnitInvMaster poData = new UnitInvMaster();
    private UnitInvSerial poSerial = new UnitInvSerial();
    private UnitInventory poDataInv = new UnitInventory();
    private Inventory poInventory = null;

    private IResult poCallback;
}
