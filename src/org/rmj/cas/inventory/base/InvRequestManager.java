/**
 * Inventory Stock Request Manager
 * Collation on Stock Request
 * This Class will Manage Stock Request for
 * Approval / Issuance / Purchasing
 * Model class InvRequest into Array
 *
 * @author Maynard Valencia
 * @since 2024.08.16
 */
package org.rmj.cas.inventory.base;

import com.mysql.jdbc.Connection;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.input.MouseEvent;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
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
import org.rmj.cas.inventory.base.views.SubUnitController;
import org.rmj.cas.inventory.others.pojo.UnitInvRequestOthers;
import org.rmj.cas.inventory.pojo.UnitInvRequestDetail;
import org.rmj.cas.inventory.pojo.UnitInvRequestMaster;
import org.rmj.lp.parameter.agent.XMBranch;
import org.rmj.appdriver.agentfx.callback.IMasterDetail;
import org.rmj.appdriver.constants.UserRight;
import org.rmj.lp.parameter.agent.XMCategory;
import org.rmj.lp.parameter.agent.XMInventory;
import org.rmj.lp.parameter.agent.XMInventoryType;

public class InvRequestManager {

    //Member Variables
    private GRider poGRider = null;
    private InvRequest poInvRequest = null;
    private String psUserIDxx = "";

    private String psBranchCd = "";
    private String psWarnMsg = "";
    private boolean pbWithParent = false;
    private int pnFormType = 0;
    private int pnEditMode;
    private int pnTranStat = 0;
    private IMasterDetail poCallBack;

    private ArrayList<InvRequest> InvRequestListManager;

    private final String pxeModuleName = "InvRequestManager";
    private double xOffset = 0;
    private double yOffset = 0;

    public InvRequestManager(GRider foGRider, String fsBranchCD, boolean fbWithParent) {
        this.poGRider = foGRider;

        if (foGRider != null) {
            this.pbWithParent = fbWithParent;
            this.psBranchCd = fsBranchCD;

            InvRequestListManager = new ArrayList<>();

            this.psUserIDxx = foGRider.getUserID();
            pnEditMode = EditMode.UNKNOWN;
        }
    }

    public void setBranch(String string) {
        psBranchCd = string;
    }

    public void setFormType(int foFormType) {
        pnFormType = foFormType;
    }

    public void setWithParent(boolean bln) {
        pbWithParent = bln;
    }

    public String getMessage() {
        return psWarnMsg;
    }

    public String getSQ_Master() {
        return MiscUtil.makeSelect(new UnitInvRequestMaster());
    }

    public void addInvRequestData(InvRequest poData) {
        InvRequestListManager.add(poData);
    }

    public void removeInvRequest(int fnIndex) {
        if (fnIndex >= 0 && fnIndex < InvRequestListManager.size()) {
            InvRequestListManager.remove(fnIndex);
        }
    }

    public InvRequest getInvRequest(int fnIndex) {
        if (fnIndex >= 0 && fnIndex < InvRequestListManager.size()) {
            return InvRequestListManager.get(fnIndex);
        }
        return null;
    }

    public int getInvRequestCount() {
        return InvRequestListManager.size();
    }

    public void setTranStat(int fnValue) {
        this.pnTranStat = fnValue;
    }

    //callback methods
    public void setCallBack(IMasterDetail foCallBack) {
        poCallBack = foCallBack;

    }

    public int getEditMode() {
        return pnEditMode;
    }

    public int ItemCount(int fnIndex) {
        return InvRequestListManager.get(fnIndex).ItemCount();
    }

    public Inventory GetInventory(int fnIndex, String fsValue, boolean fbByCode, boolean fbSearch) {
        return InvRequestListManager.get(fnIndex).GetInventory(fsValue, fbByCode, fbSearch);
    }

    public XMBranch GetBranch(int fnIndex, String fsValue, boolean fbByCode) {
        return InvRequestListManager.get(fnIndex).GetBranch(fsValue, fbByCode);
    }

    public XMInventoryType GetInventoryType(int fnIndex, String fsValue, boolean fbByCode) {
        return InvRequestListManager.get(fnIndex).GetInventoryType(fsValue, fbByCode);
    }

    public Object getMaster(int fnIndex, int fnCol) {
        if (pnEditMode == EditMode.UNKNOWN) {
            return null;
        } else {
            return InvRequestListManager.get(fnIndex).getMaster(fnCol);
        }
    }

    public Object getMaster(int fnIndex, String fsCol) {
        return InvRequestListManager.get(fnIndex).getMaster(fsCol);
    }

    public void setDetail(int fnIndex, int fnRow, int fnCol, Object foData) {
        if (pnEditMode != EditMode.UNKNOWN) {
            //nApproved  && nCancelld //nIssueQty //nOrderQty(Purchase) // 
            InvRequestListManager.get(fnIndex).setDetail(fnRow, fnCol, foData);
        }
    }

    public void setDetail(int fnIndex, int fnRow, String fsCol, Object foData) {
        //nApproved = 10 && nCancelld = 11 //nIssueQty = 12 //nOrderQty = 13
        InvRequestListManager.get(fnIndex).setDetail(fnRow, fsCol, foData);
    }

    public void setDetailOther(int fnIndex, int fnRow, int fnCol, Object foData) {
        if (pnEditMode != EditMode.UNKNOWN) {
            //nApproved  && nCancelld //nIssueQty //nOrderQty(Purchase) // 
            InvRequestListManager.get(fnIndex).setDetailOther(fnRow, fnCol, foData);
        }
    }

    public void setDetailOther(int fnIndex, int fnRow, String fsCol, Object foData) {
        //nApproved = 10 && nCancelld = 11 //nIssueQty = 12 //nOrderQty = 13
        InvRequestListManager.get(fnIndex).setDetailOther(fnRow, fsCol, foData);
    }

    public Object getDetail(int fnIndex, int fnRow, int fnCol) {
        if (pnEditMode == EditMode.UNKNOWN) {
            return null;
        } else {
            return InvRequestListManager.get(fnIndex).getDetail(fnRow, fnCol);
        }
    }

    public Object getDetail(int fnIndex, int fnRow, String fsCol) {
        return InvRequestListManager.get(fnIndex).getDetail(fnRow, fsCol);
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

    public boolean updateRecord(int fnIndex) {

        if (InvRequestListManager.get(fnIndex).updateRecord()) {
            pnEditMode = EditMode.UPDATE;
            return true;
        } else {
            return false;
        }
    }

    public Object getDetailOthers(int fnIndex, int fnRow, String fsCol) {
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
            case "nApproved":
            case "nIssueQty":
            case "nOrderQty":
                return InvRequestListManager.get(fnIndex).getDetailOthers(fnRow, fsCol);
            default:
                return null;
        }
    }

    public boolean loadInvRequestList() {
        Connection loConn = null;
        loConn = setConnection();
        InvRequest poData = null;
        try {

            if (pnFormType > 3 || pnFormType < 0) {
                psWarnMsg = "Unconfigured system detected!";
                return false;
            }
            InvRequestListManager = new ArrayList<>();
            ResultSet loRS = poGRider.executeQuery(getSQ_Master());

            for (int lnCtr = 1; lnCtr <= MiscUtil.RecordCount(loRS); lnCtr++) {
                loRS.absolute(lnCtr);
                poData = loadInvRequestTransaction(loRS.getString("sTransNox"));

                if (poData != null) {
                    InvRequestListManager.add(poData);
                }

            }
            if (InvRequestListManager.size() <= 0) {
                psWarnMsg = "No item to retrieve! " + poData.getMessage();
                pnEditMode = EditMode.UNKNOWN;
                return false;
            }

            pnEditMode = EditMode.READY;
            return true;

        } catch (SQLException ex) {
            Logger.getLogger(InvRequestManager.class
                    .getName()).log(Level.SEVERE, null, ex);

            psWarnMsg = ex.getMessage();
            return false;
        }
//        return true;
    }

    public boolean loadInvRequestListByBranch(String fsBranchCd) {
        Connection loConn = null;
        loConn = setConnection();
        InvRequest poData = null;
        try {
            InvRequestListManager = new ArrayList<>();
            if (pnFormType > 3 || pnFormType < 0) {
                psWarnMsg = "Unconfigured System detected!";
                return false;
            }
            String lsSQL = MiscUtil.addCondition(getSQ_Master(), "sBranchCd = " + SQLUtil.toSQL(fsBranchCd));
            ResultSet loRS = poGRider.executeQuery(lsSQL);

            for (int lnCtr = 1; lnCtr <= MiscUtil.RecordCount(loRS); lnCtr++) {
                loRS.absolute(lnCtr);
                poData = loadInvRequestTransaction(loRS.getString("sTransNox"));

                if (poData != null) {
                    InvRequestListManager.add(poData);
                }

            }
            if (InvRequestListManager.size() <= 0) {
                psWarnMsg = "No item to retrieve! ";
                pnEditMode = EditMode.UNKNOWN;
                return false;
            }
            pnEditMode = EditMode.READY;
            return true;

        } catch (SQLException ex) {
            Logger.getLogger(InvRequestManager.class
                    .getName()).log(Level.SEVERE, null, ex);
            psWarnMsg = ex.getMessage();
            return false;
        }
//        return true;
    }

    private InvRequest loadInvRequestTransaction(String fsTransNox) throws SQLException {
        InvRequest instance = new InvRequest(poGRider, psBranchCd, !pbWithParent);
        psWarnMsg = "";
        if (instance.openTransaction(fsTransNox)) {
            int lnItemCount = instance.ItemCount() - 1;
            if (lnItemCount > 0) {

                int lnItem = 0;
                for (int lnCtr = 0; lnCtr <= lnItemCount; lnCtr++) {
                    double lnQuantity = Double.valueOf(instance.getDetail(lnCtr, "nQuantity").toString());
                    double lnApproved = Double.valueOf(instance.getDetail(lnCtr, "nApproved").toString());
                    double lnCancelld = Double.valueOf(instance.getDetail(lnCtr, "nCancelld").toString());
                    double lnIssueQty = Double.valueOf(instance.getDetail(lnCtr, "nIssueQty").toString());
                    double lnOrderQty = Double.valueOf(instance.getDetail(lnCtr, "nOrderQty").toString());

                    String lsStockid = (String) instance.getDetail(lnCtr, "sStockIDx");
                    //validator for form
                    switch (pnFormType) {
                        case 0://approval form
                            if (lnQuantity - (lnApproved + lnCancelld) > 0) {
                                lnItem++;
                                continue;
                            } else {
                                instance.deleteDetail(lnCtr);
                                lnCtr -= 1;
                                lnItemCount -= 1;
                            }
                            break;
                        case 1://issuanceform
                            if (lnApproved - (lnIssueQty + lnOrderQty) > 0) {
                                lnItem++;
                                continue;
                            } else {
                                instance.deleteDetail(lnCtr);
                                lnCtr -= 1;
                                lnItemCount -= 1;
                            }
                            break;

                        case 2://purchasing form
                            if (lnApproved - (lnIssueQty + lnOrderQty) > 0) {
                                lnItem++;
                                continue;
                            } else {
                                instance.deleteDetail(lnCtr);
                                lnCtr -= 1;
                                lnItemCount -= 1;
                            }
                            break;

                    }
                }

                if (lnItem >= 1) {
                    return instance;
                }
            }

        } else {
            psWarnMsg = instance.getMessage();
        }
        return null;
    }

    public boolean saveTransaction(int fnIndex) {
        String lsSQL = "";
        boolean lbUpdate = false;

        //validate
//        if (!isEntryOkay(fnIndex)) {
//            return false;
//        }
        //save the selected modified data
        if (!pbWithParent) {
            poGRider.beginTrans();
        }
        lbUpdate = InvRequestListManager.get(fnIndex).saveTransaction();
        if (lbUpdate) {
            switch (pnFormType) {
                case 1:
                case 2:

            }
        }
        //other function before commit
        if (!pbWithParent) {
            if (!InvRequestListManager.get(fnIndex).getErrMsg().isEmpty()) {
                poGRider.rollbackTrans();
            } else {
                poGRider.commitTrans();
            }
        }

        return lbUpdate;
    }

    public XMCategory GetCategory(String fsValue, boolean fbByCode) {
        if (fbByCode && fsValue.equals("")) {
            return null;
        }

        XMCategory instance = new XMCategory(poGRider, psBranchCd, true);
        if (instance.browseRecord(fsValue, fbByCode)) {
            return instance;
        } else {
            return null;
        }
    }

    public InvMaster GetIssueInventory(String fsValue, boolean fbByCode) {
        if (fbByCode && fsValue.equals("")) {
            return null;
        }

        InvMaster instance = new InvMaster(poGRider, psBranchCd, true);
        if (instance.SearchInventory(fsValue, fbByCode, true)) {
            return instance;
        } else {
            return null;
        }
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

    public InvRequest GetInvRequest(String fsValue, boolean fbByCode) {
        if (fbByCode && fsValue.equals("")) {
            return null;
        }

        InvRequest instance = new InvRequest(poGRider, psBranchCd, true);
        if (instance.BrowseRecord(fsValue, fbByCode)) {
            return instance;
        } else {
            return null;
        }
    }

    public boolean isEntryOkay(int fnIndex) {
        psWarnMsg = "";
        //serves as validation check value
        int lnCtr = 0;
        int lnModified = 0;
        double lnQuantity = 0;
        double lnApproved = 0;
        double lnCancelld = 0;
        double lnIssueQty = 0;
        double lnOldIssueQty = 0;
        double lnOrderQty = 0;
        double lnOldOrderQty = 0;
        double lnOnTrans = 0;
        String lsStockID = "";
        String lsBarcode = "";
        boolean lbReqApproval = false;
        int lnItem = InvRequestListManager.get(fnIndex).ItemCount();

        switch (pnFormType) {

            case 0://Approval Form
                for (lnCtr = 0; lnCtr <= lnItem - 1; lnCtr++) {
                    lnQuantity = Double.valueOf(InvRequestListManager.get(fnIndex).getDetail(lnCtr, "nQuantity").toString());
                    lnApproved = Double.valueOf(InvRequestListManager.get(fnIndex).getDetail(lnCtr, "nApproved").toString());
                    lnCancelld = Double.valueOf(InvRequestListManager.get(fnIndex).getDetail(lnCtr, "nCancelld").toString());
                    if (lnApproved > 0) {
                        lnModified++;
                    }
                    if (lnApproved > lnQuantity) {
                        lbReqApproval = true;
                        break;
                    }
                }

                break;
            case 1://Issuance Form
                for (lnCtr = 0; lnCtr <= lnItem - 1; lnCtr++) {
                    lnOnTrans = Double.valueOf(InvRequestListManager.get(fnIndex).getDetail(lnCtr, "nOnTranst").toString());
                    lnQuantity = Double.valueOf(InvRequestListManager.get(fnIndex).getDetail(lnCtr, "nQuantity").toString());
                    lnApproved = Double.valueOf(InvRequestListManager.get(fnIndex).getDetail(lnCtr, "nApproved").toString());
                    lnCancelld = Double.valueOf(InvRequestListManager.get(fnIndex).getDetail(lnCtr, "nCancelld").toString());
                    lnOldIssueQty = Double.valueOf(InvRequestListManager.get(fnIndex).getDetail(lnCtr, "nIssueQty").toString());
                    lnIssueQty = Double.valueOf(InvRequestListManager.get(fnIndex).getDetailOthers(lnCtr, "nIssueQty").toString());
                    lnOrderQty = Double.valueOf(InvRequestListManager.get(fnIndex).getDetail(lnCtr, "nOrderQty").toString());
                    lsStockID = (String) InvRequestListManager.get(fnIndex).getDetail(lnCtr, "sStockIDx");
                    lsBarcode = (String) InvRequestListManager.get(fnIndex).getDetailOthers(lnCtr, "sBarCodex");

                    InvMaster loInvMaster = GetIssueInventory(lsStockID, true);

                    if (lnIssueQty > 0) {
                        lnModified++;
//                        if (lnApproved < (lnOldIssueQty + lnOrderQty)) {
//                            lbReqApproval = true;
//                            break;
//                        }
                    }

                    if (lnApproved < (lnOrderQty + lnOldIssueQty)) {
                        psWarnMsg = "Unable to save. The Issue quantity for an item ( " + lsBarcode + " ) exceeds the approved quantity."
                                + " Remaining qty : " + (lnApproved - lnOnTrans);
                        return false;
                    }
                    if (loInvMaster != null) {
                        double nQtyOnHand = Double.valueOf(loInvMaster.getMaster("nQtyOnHnd").toString());
                        if (nQtyOnHand < lnIssueQty) {
                            psWarnMsg = "Unable to save. The issued quantity for an item ( " + lsBarcode + " )  exceeds the available quantity.";
                            return false;
                        }

                    }
                }
                break;
            case 2: //PO Form
                for (lnCtr = 0; lnCtr <= lnItem - 1; lnCtr++) {
                    lnOnTrans = Double.valueOf(InvRequestListManager.get(fnIndex).getDetail(lnCtr, "nOnTranst").toString());
                    lnQuantity = Double.valueOf(InvRequestListManager.get(fnIndex).getDetail(lnCtr, "nQuantity").toString());
                    lnApproved = Double.valueOf(InvRequestListManager.get(fnIndex).getDetail(lnCtr, "nApproved").toString());
                    lnCancelld = Double.valueOf(InvRequestListManager.get(fnIndex).getDetail(lnCtr, "nCancelld").toString());
                    lnIssueQty = Double.valueOf(InvRequestListManager.get(fnIndex).getDetail(lnCtr, "nIssueQty").toString());
                    lnOrderQty = Double.valueOf(InvRequestListManager.get(fnIndex).getDetailOthers(lnCtr, "nOrderQty").toString());
                    lnOldOrderQty = Double.valueOf(InvRequestListManager.get(fnIndex).getDetail(lnCtr, "nOrderQty").toString());
                    lsBarcode = (String) InvRequestListManager.get(fnIndex).getDetailOthers(lnCtr, "sBarCodex");

                    if (lnOrderQty > 0) {
                        lnModified++;
                    }

                    if (lnApproved < (lnIssueQty + lnOldOrderQty)) {
                        psWarnMsg = "Unable to save. The Purchase quantity for an item ( " + lsBarcode + " ) exceeds the approved quantity."
                                + " Remaining quantity : " + (lnApproved - lnOnTrans);
                        return false;
                    }
                }
                break;
            default:
                psWarnMsg = "Unconfigured system detected!";
                return false;
        }
        if (lnModified <= 0) {
            psWarnMsg = "Unable to save. No items have been modified in the transaction record.";
            return false;
        }
        if (lbReqApproval) {
            if (poGRider.getUserLevel() < UserRight.SUPERVISOR) {
                JSONObject loJSON = showFXDialog.getApproval(poGRider);

                if (loJSON != null) {
                    if ((int) loJSON.get("nUserLevl") < UserRight.SUPERVISOR) {
                        psWarnMsg = "Only managerial accounts can approved transactions.(Authentication failed!!!)";
                        return false;
                    }
                    return false;
                }
            }
        }
        return true;
    }
}
//
//        private boolean showPurchaseOrder(int fnIndex) {
//
//        boolean lbHasDiscrepancy = false;
//         InvDiscrepancyController loInvDiscrepancy = new InvDiscrepancyController();
//        InvTransferDiscrepancy loInvTransDiscrepancy = new InvTransferDiscrepancy(poGRider, poGRider.getBranchCode(), true);
//        loInvTransDiscrepancy.newTransaction();
//        loInvTransDiscrepancy.setMaster("sSourceCD", InvConstants.ACCEPT_DELIVERY);
//        loInvTransDiscrepancy.setMaster("sSourceNo", poData.getTransNox());
//        for (int lnCtr = 0; lnCtr <= paDetail.size() - 1; lnCtr++) {
//            if (!paDetail.get(lnCtr).getQuantity().equals(paDetail.get(lnCtr).getReceived())) {
//                //to addnew row to end if not last
//                loInvTransDiscrepancy.addDetail();
//                int lnRow = loInvTransDiscrepancy.ItemCount() - 1;
//                //compute the qty of missing
//                Double DiscripancyQty = Math.abs((Double) paDetail.get(lnCtr).getQuantity() - (Double) paDetail.get(lnCtr).getReceived());
//
//                loInvTransDiscrepancy.setDetail(lnRow, "nQuantity", DiscripancyQty);
//                loInvTransDiscrepancy.setDetail(lnRow, "dExpiryDt", paDetail.get(lnCtr).getDateExpiry());
//                loInvTransDiscrepancy.setItemDetail(lnRow, 3, paDetail.get(lnCtr).getStockIDx(), false, true);
//                lbHasDiscrepancy = true;
//
//                if (lnCtr != paDetail.size() - 1) {
//                }
//            }
//        }
//        if (lbHasDiscrepancy) {
//            loInvDiscrepancy.setInvDiscrepancy(loInvTransDiscrepancy);
//            loInvDiscrepancy.setGRider(poGRider);
//            FXMLLoader fxmlLoader = new FXMLLoader();
//            fxmlLoader.setLocation(getClass().getResource("views/InvDiscrepancy.fxml"));
//            fxmlLoader.setController(loInvDiscrepancy);
//            try {
//                Parent parent = fxmlLoader.load();
//
//                Stage stage = new Stage();
//
//                parent.setOnMousePressed(new EventHandler<MouseEvent>() {
//                    @Override
//                    public void handle(MouseEvent event) {
//                        xOffset = event.getSceneX();
//                        yOffset = event.getSceneY();
//                    }
//                });
//                parent.setOnMouseDragged(new EventHandler<MouseEvent>() {
//                    @Override
//                    public void handle(MouseEvent event) {
//                        stage.setX(event.getScreenX() - xOffset);
//                        stage.setY(event.getScreenY() - yOffset);
//
//                    }
//                });
//
//                Scene scene = new Scene(parent);
//                stage.initModality(Modality.APPLICATION_MODAL);
//                stage.initStyle(StageStyle.UNDECORATED);
//                stage.setScene(scene);
//                stage.showAndWait();
//
//                if (!loInvDiscrepancy.isCancelled()) {
//                    return true;
//                } else {
//                    return false;
//                }
//
//            } catch (IOException ex) {
//                ShowMessageFX.Error(ex.getMessage(), pxeModuleName, "Please inform MIS department.");
//                return false;
//            }
//        }
//        return true;
//    }
//        

