/**
 * Maynard Valencia
 *
 * @since 2024-05-15
 */
package org.rmj.cas.inventory.base.views;

import com.sun.javafx.scene.control.skin.TableHeaderRow;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.beans.property.ReadOnlyBooleanPropertyBase;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.data.JsonDataSource;
import net.sf.jasperreports.view.JasperViewer;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.rmj.appdriver.GRider;
import org.rmj.appdriver.MiscUtil;
import org.rmj.appdriver.SQLUtil;
import org.rmj.appdriver.agentfx.CommonUtils;
import org.rmj.appdriver.agentfx.ShowMessageFX;
import org.rmj.appdriver.agentfx.ui.showFXDialog;
import org.rmj.appdriver.constants.EditMode;
import org.rmj.appdriver.constants.TransactionStatus;
import org.rmj.appdriver.constants.UserRight;
import org.rmj.cas.inventory.base.InvTransferDiscrepancy;
import org.rmj.lp.parameter.agent.XMBranch;

public class InvDiscrepancyController implements Initializable {

    private static GRider poGRider;
    private final String pxeModuleName = "InvDiscrepancyController";

    private InvTransferDiscrepancy poTrans = null;
    private boolean pbCancelled;
    private String psValue;
    private int pnRow;
    private int pnIndex = -1;
    private String psOldRec = "";

    private boolean pbLoaded = false;

    @FXML
    private Button btnExit;
    @FXML
    private TextField txtField01;
    @FXML
    private TextField txtField02;
    @FXML
    private TextField txtField03;
    @FXML
    private TextArea txtField04;
    @FXML
    private Button btnOk;
    @FXML
    private Button btnCancel;
    @FXML
    private TableView table;
    @FXML
    private TableColumn index01;
    @FXML
    private TableColumn index02;
    @FXML
    private TableColumn index03;
    @FXML
    private TableColumn index04;
    @FXML
    private TableColumn index05;
    @FXML
    private TableColumn index06;
    @FXML
    private TableColumn index07;
    @FXML
    private Label Label12;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        pbCancelled = true;
        psValue = "";

        btnExit.setOnAction(this::cmdButton_Click);
        btnOk.setOnAction(this::cmdButton_Click);
        btnCancel.setOnAction(this::cmdButton_Click);

        initGrid();
        loadRecord();
        loadDetail2Grid();

        txtField04.focusedProperty().addListener(txtArea_Focus);

        pbLoaded = true;
    }

    public void cmdButton_Click(ActionEvent event) {
        String lsButton = ((Button) event.getSource()).getId();

        switch (lsButton) {
            case "btnExit":
            case "btnCancel":

                CommonUtils.closeStage(btnExit);
                break;
            case "btnOk":
                if (poTrans.saveTransaction()) {
                    ShowMessageFX.Information(null, pxeModuleName, "Transaction Discrepancy saved successfuly.");
                    if (!psOldRec.equals("")) {
                        if (poGRider.getUserLevel() <= UserRight.ENCODER) {
                            JSONObject loJSON = showFXDialog.getApproval(poGRider);

                            if (loJSON == null) {
                                ShowMessageFX.Warning("Approval failed.", pxeModuleName, "Unable to save transaction discrepancy");
                            }

                            if ((int) loJSON.get("nUserLevl") <= UserRight.ENCODER) {
                                ShowMessageFX.Warning("User account has no right to approve.", pxeModuleName, "Unable to save transaction discrepancy");
                                return;
                            }
                        }

                        if (ShowMessageFX.YesNo(null, pxeModuleName, "Do you want to confirm this transaction discrepancy?") == true) {
                            if (poTrans.closeTransaction(psOldRec)) {
                                ShowMessageFX.Information(null, pxeModuleName, "Transaction Discrepancy confirmed successfully.");

                                if (ShowMessageFX.YesNo(null, pxeModuleName, "Do you want to print this transaction discrepancy?") == true) {
                                    if (!printTransfer()) {
                                        return;
                                    }
                                }
                            } else {
                                ShowMessageFX.Warning(poTrans.getErrMsg(), pxeModuleName, "Unable to confirm transaction discrepancy.");
                            }
                        }
                    } else {
                        ShowMessageFX.Warning(null, pxeModuleName, "Unable to confirm! Transaction Discrepancy");
                    }
                    pbCancelled = false;
                    CommonUtils.closeStage(btnOk);
                    break;
                } else {
                    if (!poTrans.getErrMsg().equals("")) {
                        ShowMessageFX.Error(poTrans.getErrMsg(), pxeModuleName, "Please inform MIS Department.");
                    } else {
                        ShowMessageFX.Warning(poTrans.getMessage(), pxeModuleName, "Please verify your entry.");
                    }
                    return;
                }

            default:
                ShowMessageFX.Warning(null, pxeModuleName, "Button with name " + lsButton + " not registered.");
        }
    }

    private void initGrid() {
        index01.setStyle("-fx-alignment: CENTER;");
        index02.setStyle("-fx-alignment: CENTER-LEFT;");
        index03.setStyle("-fx-alignment: CENTER-LEFT;");
        index04.setStyle("-fx-alignment: CENTER-LEFT;");
        index05.setStyle("-fx-alignment: CENTER-LEFT;");
        index06.setStyle("-fx-alignment: CENTER;");
        index07.setStyle("-fx-alignment: CENTER-LEFT;");

        index01.setCellValueFactory(new PropertyValueFactory<org.rmj.cas.inventory.base.views.TableModel, String>("index01"));
        index02.setCellValueFactory(new PropertyValueFactory<org.rmj.cas.inventory.base.views.TableModel, String>("index02"));
        index03.setCellValueFactory(new PropertyValueFactory<org.rmj.cas.inventory.base.views.TableModel, String>("index03"));
        index04.setCellValueFactory(new PropertyValueFactory<org.rmj.cas.inventory.base.views.TableModel, String>("index04"));
        index05.setCellValueFactory(new PropertyValueFactory<org.rmj.cas.inventory.base.views.TableModel, String>("index05"));
        index06.setCellValueFactory(new PropertyValueFactory<org.rmj.cas.inventory.base.views.TableModel, String>("index06"));
        index07.setCellValueFactory(new PropertyValueFactory<org.rmj.cas.inventory.base.views.TableModel, String>("index07"));

        table.widthProperty().addListener(new ChangeListener<Number>() {
            public void changed(ObservableValue<? extends Number> source, Number oldWidth, Number newWidth) {
                TableHeaderRow header = (TableHeaderRow) table.lookup("TableHeaderRow");
                header.reorderingProperty().addListener(new ChangeListener<Boolean>() {
                    @Override
                    public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
                        header.setReordering(false);
                    }
                });
            }
        });

        table.setItems(data);

        index07.setCellFactory(TextFieldTableCell.forTableColumn());
        index07.setOnEditCommit(new EventHandler<TableColumn.CellEditEvent<TableModel, String>>() {
            @Override
            public void handle(TableColumn.CellEditEvent<TableModel, String> event) {
                TableModel tableModel = event.getRowValue();
                tableModel.setIndex07(event.getNewValue());
                poTrans.setDetail(pnRow, "sRemarksx", tableModel.getIndex07());
                loadDetail2Grid();

            }
        });
    }

    public void loadDetail2Grid() {
        data.clear();

        if (poTrans == null) {
            return;
        }

        int lnRow = poTrans.ItemCount();
        for (int lnCtr = 0; lnCtr <= poTrans.ItemCount() - 1; lnCtr++) {
            data.add(new TableModel(String.valueOf(lnCtr + 1),
                    (String) poTrans.getDetailOthers(lnCtr, "sBarCodex"),
                    (String) poTrans.getDetailOthers(lnCtr, "sDescript"),
                    (String) poTrans.getDetailOthers(lnCtr, "sBrandNme"),
                    (String) poTrans.getDetailOthers(lnCtr, "sMeasurNm"),
                    String.valueOf(poTrans.getDetail(lnCtr, "nQuantity")),
                    (String) poTrans.getDetail(lnCtr, "sRemarksx"),
                    "",
                    "",
                    ""));
        }
        /*FOCUS ON FIRST ROW*/
        if (!data.isEmpty()) {
            table.getSelectionModel().select(lnRow - 1);
            table.getFocusModel().focus(lnRow - 1);

            pnRow = table.getSelectionModel().getSelectedIndex();
        }
        Label12.setText(CommonUtils.NumberFormat(Double.valueOf(poTrans.getMaster("nTranTotl").toString()), "#,##0.00"));

    }

    private void loadRecord() {
        txtField01.setText((String) poTrans.getMaster("sTransNox"));
        txtField02.setText(xsRequestFormat((Date) poTrans.getMaster("dTransact")));

        XMBranch loBranch = poTrans.GetBranch((String) poTrans.getMaster(4), true);
        if (loBranch != null) {
            txtField03.setText((String) loBranch.getMaster("sBranchNm"));
        }

        txtField04.setText((String) poTrans.getMaster("sRemarksx"));
        Label12.setText(CommonUtils.NumberFormat(Double.valueOf(poTrans.getMaster("nTranTotl").toString()), "#,##0.00"));
        psOldRec = (String) poTrans.getMaster("sTransNox");

    }

    @FXML
    private void table_Clicked(MouseEvent event) {
        pnRow = table.getSelectionModel().getSelectedIndex();
    }

    private Stage getStage() {
        return (Stage) btnOk.getScene().getWindow();
    }

    private ObservableList<TableModel> data = FXCollections.observableArrayList();

    public String getValue() {
        return psValue;
    }

    public boolean isCancelled() {
        return pbCancelled;
    }

    public void setInvDiscrepancy(InvTransferDiscrepancy foRS) {
        this.poTrans = foRS;
    }

    public static String xsRequestFormat(Date fdValue) {
        SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy");
        String date = sdf.format(fdValue);
        return date;
    }

    public static String xsRequestFormat(String fsValue) throws ParseException {
        SimpleDateFormat fromUser = new SimpleDateFormat("MM/dd/yyyy");
        SimpleDateFormat myFormat = new SimpleDateFormat("MM/dd/yyyy");
        String lsResult = "";

        try {
            lsResult = myFormat.format(fromUser.parse(fsValue));
        } catch (ParseException e) {
            ShowMessageFX.Error(e.getMessage(), "xsDateShort", "Please inform MIS Department.");
            System.exit(1);
        }

        return lsResult;
    }

    public void setGRider(GRider foGRider) {
        this.poGRider = foGRider;
    }
    final ChangeListener<? super Boolean> txtArea_Focus = (o, ov, nv) -> {
        if (!pbLoaded) {
            return;
        }

        TextArea txtField = (TextArea) ((ReadOnlyBooleanPropertyBase) o).getBean();
        int lnIndex = Integer.parseInt(txtField.getId().substring(8, 10));
        String lsValue = txtField.getText();

        if (lsValue == null) {
            return;
        }

        if (!nv) {
            /*Lost Focus*/
            switch (lnIndex) {
                case 4:
                    /*sRemarksx*/
                    if (lsValue.length() > 126) {
                        lsValue = lsValue.substring(0, 126);
                    }
                    poTrans.setMaster("sRemarksx", CommonUtils.TitleCase(lsValue));
                    txtField.setText((String) poTrans.getMaster("sRemarksx"));
                    break;
            }
        } else {
            pnIndex = -1;
            txtField.selectAll();
        }
    };

    private boolean printTransfer() {
        JSONArray json_arr = new JSONArray();
        json_arr.clear();

        for (int lnCtr = 0; lnCtr <= poTrans.ItemCount() - 1; lnCtr++) {
            JSONObject json_obj = new JSONObject();
            json_obj.put("sField01", (String) poTrans.getDetailOthers(lnCtr, "sBarCodex"));
            json_obj.put("sField02", (String) poTrans.getDetailOthers(lnCtr, "sDescript"));
            json_obj.put("sField03", (String) poTrans.getDetailOthers(lnCtr, "sMeasurNm"));
            json_obj.put("sField04", (String) poTrans.getDetailOthers(lnCtr, "sBrandNme"));
            json_obj.put("sField05", poTrans.getDetail(lnCtr, "sRemarksx") == null ? "" : (String) poTrans.getDetail(lnCtr, "sRemarksx"));
            json_obj.put("lField01", (Double) poTrans.getDetail(lnCtr, "nQuantity"));
            json_arr.add(json_obj);
        }

        String lsSQL = "SELECT sBranchNm FROM Branch WHERE sBranchCD = " + SQLUtil.toSQL((String) poTrans.getMaster("sDestinat"));
        ResultSet loRS = poGRider.executeQuery(lsSQL);

        try {
            if (loRS.next()) {
                lsSQL = loRS.getString("sBranchNm");
            } else {
                lsSQL = (String) poTrans.getMaster("sDestinat");
            }

            //Create the parameter
            Map<String, Object> params = new HashMap<>();
            params.put("sReportNm", "Inventory Discrepancy Transfer");
            params.put("sBranchNm", poGRider.getBranchName());
            params.put("sBranchCd", poGRider.getBranchCode());
            params.put("sDestinat", lsSQL);
            params.put("sTransNox", poTrans.getMaster("sTransNox").toString().substring(1));
            params.put("sReportDt", CommonUtils.xsDateLong((Date) poTrans.getMaster("dTransact")));
            params.put("sPrintdBy", System.getProperty("user.name"));
            params.put("xRemarksx", poTrans.getMaster("sRemarksx"));

            lsSQL = "SELECT sClientNm FROM Client_Master WHERE sClientID IN ("
                    + "SELECT sEmployNo FROM xxxSysUser WHERE sUserIDxx = " + SQLUtil.toSQL(poGRider.getUserID()) + ")";
            loRS = poGRider.executeQuery(lsSQL);

            if (loRS.next()) {
                params.put("sPrepared", loRS.getString("sClientNm"));
            } else {
                params.put("sPrepared", "");
            }

            InputStream stream = new ByteArrayInputStream(json_arr.toJSONString().getBytes("UTF-8"));
            JsonDataSource jrjson = new JsonDataSource(stream);

            JasperPrint _jrprint = JasperFillManager.fillReport("d:/GGC_Java_Systems/reports/InvTransferDiscrepancyPrint.jasper", params, jrjson);
            JasperViewer jv = new JasperViewer(_jrprint, false);
            jv.setVisible(true);
            jv.setAlwaysOnTop(true);
        } catch (JRException | UnsupportedEncodingException | SQLException ex) {
            poTrans.setErrMsg(ex.getMessage());
        }

        return true;
    }
}
