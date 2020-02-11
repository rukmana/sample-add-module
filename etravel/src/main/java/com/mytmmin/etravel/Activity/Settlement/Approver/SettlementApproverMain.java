package com.mytmmin.etravel.Activity.Settlement.Approver;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.mytmmin.etravel.API.APIClient;
import com.mytmmin.etravel.API.MyTravelAPIInterface;
import com.mytmmin.etravel.Adapter.Settlement.Approver.SettlementApproverListSettlementApprovalAdapter;
import com.mytmmin.etravel.DataModel.MyTravelListApproveRejectDataModel;
import com.mytmmin.etravel.DataModel.MyTravelMainDataDataModel;
import com.mytmmin.etravel.Helper;
import com.mytmmin.etravel.POJO.AllDraftNumberToyotaDevPojo;
import com.mytmmin.etravel.POJO.ApproveRejectToyotaDevPojo;
import com.mytmmin.etravel.POJO.MyTravelMainDevPojo;
import com.mytmmin.etravel.R;
import com.omega_r.libs.omegarecyclerview.OmegaRecyclerView;
import com.omega_r.libs.omegarecyclerview.pagination.OnPageRequestListener;

import java.util.ArrayList;

import cn.pedant.SweetAlert.SweetAlertDialog;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import static com.mytmmin.etravel.GlobalVariable.SETTLEMENT_APPROVER_REQUEST_DETAILS_RESULT_CODE;
import static com.mytmmin.etravel.GlobalVariable.TAG_OPEN_SETTLEMENT_APPROVER_REQUEST_DETAILS_ACTIVITY;

public class SettlementApproverMain extends AppCompatActivity implements View.OnClickListener, SwipeRefreshLayout.OnRefreshListener,
        SearchView.OnQueryTextListener, OnPageRequestListener, SettlementApproverListSettlementApprovalAdapter.Callback {

    private CheckBox checkAll;
    private Activity activity;
    private SearchView searchView;

    private SettlementApproverListSettlementApprovalAdapter adapter;
    private SwipeRefreshLayout swipe;
    OmegaRecyclerView listSettlementRecyclerView;

    private TextView totalRequestText;

    private int mCounter;
    private ArrayList<MyTravelMainDataDataModel> listData;

    private static final String TAG = SettlementApproverMain.class.getSimpleName();

    private MyTravelAPIInterface travelApiInterface;

    private static final int limit = 5;

    private int totalData=0, pages=0, returnValue=-1, successCounter=0, failureCounter=0;

    private Helper helper;

//    private MyTravelMainDataDataModel detailRequest;
    //Initiation handler

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.sa_activity_approver_main);

        travelApiInterface = APIClient.getClient().create(MyTravelAPIInterface.class);

        //init views
        initView();

    }

    private void initView(){
        activity = this;
        helper = new Helper(getApplicationContext());

        //init toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setTitleTextColor(getResources().getColor(R.color.white));
        toolbar.setNavigationIcon(R.drawable.ic_chevron_left_white_24dp);
        toolbar.setNavigationOnClickListener(this);

        swipe = findViewById(R.id.swipe_refresh);
        swipe.setOnRefreshListener(this);

        Button approveButton = findViewById(R.id.button_call_to_action_approve);
        Button rejectButton = findViewById(R.id.button_call_to_action_reject);
        approveButton.setOnClickListener(this);
        rejectButton.setOnClickListener(this);

        listSettlementRecyclerView = findViewById(R.id.recyclerview_list_trip);
        listSettlementRecyclerView.setLayoutManager(new LinearLayoutManager(getBaseContext()));
        listSettlementRecyclerView.setPaginationCallback(this);

        adapter = new SettlementApproverListSettlementApprovalAdapter();
        adapter.setCallback(this);
        listSettlementRecyclerView.setAdapter(adapter);
        listSettlementRecyclerView.showProgressPagination();

        checkAll = findViewById(R.id.checkbox_select_all);
        checkAll.setOnClickListener(this);

        totalRequestText = findViewById(R.id.total_number_of_request);

        swipe.post(new Runnable() {

            @Override
            public void run() {

                if(swipe != null) {
                    swipe.setRefreshing(true);
                }
                if(pages>0){
                    refreshData();
                }
            }
        });

    }

    //Pagination handler
    @Override
    public void onPageRequest(int page) {

        pages = page;

        if (pages == 0 || pages*limit < totalData){
            if (searchView != null){
                if (searchView.isIconified()){
                    mCounter++;
                    initData("on page request not null", pages);
                }
            }
            else{
                mCounter++;
                initData("on page request null", pages);
            }
        }

    }

    @Override
    public int getPagePreventionForEnd() {

        if (searchView != null && searchView.isIconified()){
            if (adapter.getItemCount()>0){
                returnValue = 0;
            }
            else{
                returnValue = -1;
            }
        }
        else{
            returnValue = -1;
        }

        return returnValue;
    }

    @Override
    public void onRetryClicked() {

        if (!searchView.isIconified()){
            searchView.setIconified(true);
            listSettlementRecyclerView.showProgressPagination();
        }

        refreshData();

    }

    //API calls

    private void initData(String functionname, final int page){

        swipe.setRefreshing(true);

        Call<MyTravelMainDevPojo> call = travelApiInterface.getAllRequestApprover("09105014", "settlement", Integer.toString(page+1), Integer.toString(limit));
        call.enqueue(new Callback<MyTravelMainDevPojo>() {
            @Override
            public void onResponse(@NonNull Call<MyTravelMainDevPojo> call, @NonNull Response<MyTravelMainDevPojo> response) {

                MyTravelMainDevPojo resource = response.body();

                if (resource!=null && resource.success){
                    listData = new ArrayList<>();
                    totalData = resource.totalData;
                    listData = resource.data;

                    for (int i=0; i<listData.size(); i++){
                        //sanitize money
                        listData.get(i).mainDataTsAllowance.setTsBalanceInIdr(helper.sanitizeMoneyHelper(listData.get(i).mainDataTsAllowance.tsBalanceInIdr, false));
                        listData.get(i).mainDataTsAllowance.setTsTotalDeductionInIdr(helper.sanitizeMoneyHelper(listData.get(i).mainDataTsAllowance.tsTotalDeductionInIdr, false));
                        listData.get(i).mainDataTsAllowance.tsTotalInIdr.setTsTotalProposal(helper.sanitizeMoneyHelper(listData.get(i).mainDataTsAllowance.tsTotalInIdr.tsTotalProposal, false));
                        listData.get(i).mainDataTsAllowance.tsTotalInIdr.setTsTotalSettlement(helper.sanitizeMoneyHelper(listData.get(i).mainDataTsAllowance.tsTotalInIdr.tsTotalSettlement, false));
                        listData.get(i).setMainDataUsdExchangeRate(helper.sanitizeMoneyHelper(listData.get(i).mainDataUsdExchangeRate, true));
                        listData.get(i).setMainDataJpyExchangeRate(helper.sanitizeMoneyHelper(listData.get(i).mainDataJpyExchangeRate, true));

                        //sanitize date
                        for (int j=0; j<listData.get(i).mainDataDestinations.size(); j++){
                            listData.get(i).mainDataDestinations.get(j).setMyTravelDestinationFrom(helper.sanitizeDateHelper(listData.get(i).mainDataDestinations.get(j).myTravelDestinationFrom));
                            listData.get(i).mainDataDestinations.get(j).setMyTravelDestinationUntil(helper.sanitizeDateHelper(listData.get(i).mainDataDestinations.get(j).myTravelDestinationUntil));
                        }

                        //sanitize allowance per item (karena ini settlement, auto ignore travel proposal data)
                        helper.sanitizeTsDetailPerItem(listData.get(i).mainDataTsAllowance.tsDirectAllowance.tsDirectDailyAllowance);
                        helper.sanitizeTsDetailPerItem(listData.get(i).mainDataTsAllowance.tsDirectAllowance.tsDirectPreparationAllowance);
                        helper.sanitizeTsDetailPerItem(listData.get(i).mainDataTsAllowance.tsDirectAllowance.tsDirectWinterAllowance);
                        helper.sanitizeTsDetailPerItem(listData.get(i).mainDataTsAllowance.tsSuspenseAllowance.tsSuspenseDomesticLandTransportAllowance);
                        helper.sanitizeTsDetailPerItem(listData.get(i).mainDataTsAllowance.tsSuspenseAllowance.tsSuspenseFiscalTaxesAllowance);
                        helper.sanitizeTsDetailPerItem(listData.get(i).mainDataTsAllowance.tsSuspenseAllowance.tsSuspenseHotelAllowance);
                        helper.sanitizeTsDetailPerItem(listData.get(i).mainDataTsAllowance.tsSuspenseAllowance.tsSuspenseMiscellaneousAllowance);
                        helper.sanitizeTsDetailPerItem(listData.get(i).mainDataTsAllowance.tsSuspenseAllowance.tsSuspenseOverseasLandTransportAllowance);

                        //sanitize allowance per item (karena ini settlement, auto ignore travel proposal data)
                        for (int j=0; j< listData.get(i).mainDataTsDestinationDetail.size(); j++){
                            helper.sanitizeTsDetailPerItem(listData.get(i).mainDataTsDestinationDetail.get(j).tsDestinationDetailDailyAllowance);
                            helper.sanitizeTsDetailPerItem(listData.get(i).mainDataTsDestinationDetail.get(j).tsDestinationDetailDomesticLandTransportAllowance);
                            helper.sanitizeTsDetailPerItem(listData.get(i).mainDataTsDestinationDetail.get(j).tsDestinationDetailFiscalTaxesAllowance);
                            helper.sanitizeTsDetailPerItem(listData.get(i).mainDataTsDestinationDetail.get(j).tsDestinationDetailHotelAllowance);
                            helper.sanitizeTsDetailPerItem(listData.get(i).mainDataTsDestinationDetail.get(j).tsDestinationDetailMiscellaneousAllowance);
                            helper.sanitizeTsDetailPerItem(listData.get(i).mainDataTsDestinationDetail.get(j).tsDestinationDetailOverseasLandTransportAllowance);
                            helper.sanitizeTsDetailPerItem(listData.get(i).mainDataTsDestinationDetail.get(j).tsDestinationDetailPreparationAllowance);
                            helper.sanitizeTsDetailPerItem(listData.get(i).mainDataTsDestinationDetail.get(j).tsDestinationDetailWinterAllowance);
                        }

                    }

                    adapter.addValues(activity, listData);
                    swipe.setRefreshing(false);
                    checkAll.setEnabled(true);
                    totalRequestText.setText(""+totalData);
                    adapter.totalDataSize = totalData;
                    if(page == 0){
                        listSettlementRecyclerView.scrollToPosition(0);
                    }
                }
                else{
                    swipe.setRefreshing(false);
                    listSettlementRecyclerView.showErrorPagination();
                }


            }

            @Override
            public void onFailure(@NonNull Call<MyTravelMainDevPojo> call, @NonNull Throwable t) {
                call.cancel();

                swipe.setRefreshing(false);
                listSettlementRecyclerView.showErrorPagination();

            }
        });

    }

    private void approveRejectRequest (final String type, final String action, final SweetAlertDialog sDialog, final int i){

        if(i<adapter.listApproveReject.size()){

            final Call<ApproveRejectToyotaDevPojo> call = travelApiInterface.approveRejectRequest(adapter.listApproveReject.get(i).approveRejectDraftNumber, "09105014", type, action);

            call.enqueue(new Callback<ApproveRejectToyotaDevPojo>() {
                @Override
                public void onResponse(@NonNull Call<ApproveRejectToyotaDevPojo> call, @NonNull Response<ApproveRejectToyotaDevPojo> response) {

                    ApproveRejectToyotaDevPojo resource = response.body();

                    if (resource != null && resource.success) {
                        postApproveRejectIncrement(true, resource, i);
                    } else {
                        postApproveRejectIncrement(false, resource, i);
                    }

                    approveRejectRequest(type, action, sDialog, i+1);
                }

                @Override
                public void onFailure(@NonNull Call<ApproveRejectToyotaDevPojo> call, @NonNull Throwable t) {
                    postApproveRejectIncrement(false, null, i);
                    call.cancel();

                    approveRejectRequest(type, action, sDialog, i+1);

                    Toast.makeText(getApplicationContext(), t.getMessage(), Toast.LENGTH_LONG).show();

                }
            });
        }
        else{
            Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                public void run() {
                    //nothing else to be processed, show dialog to user
                    if(successCounter>0 && failureCounter==0){
                        //all success
                        sDialog.setTitleText("Done!")
                                .setContentText(successCounter+" requests has been processed!")
                                .setConfirmText("OK")
                                .showCancelButton(false)
                                .setCancelClickListener(null)
                                .setConfirmClickListener(null)
                                .changeAlertType(SweetAlertDialog.SUCCESS_TYPE);
                    }
                    else if (successCounter>0 && failureCounter>0){
                        //some request success
                        sDialog.setTitleText("Something Happened!")
                                .setContentText(failureCounter+" requests can't be processed, please contact admin!\n")
                                .setConfirmText("OK")
                                .showCancelButton(false)
                                .setCancelClickListener(null)
                                .setConfirmClickListener(null)
                                .changeAlertType(SweetAlertDialog.WARNING_TYPE);
                    }
                    else if (successCounter==0 && failureCounter>0){
                        //all request failure
                        sDialog.setTitleText("Error!")
                                .setContentText(failureCounter+" requests can't be processed, please contact admin!\n")
                                .setConfirmText("OK")
                                .showCancelButton(false)
                                .setCancelClickListener(null)
                                .setConfirmClickListener(null)
                                .changeAlertType(SweetAlertDialog.ERROR_TYPE);
                    }

                    successCounter=0;
                    failureCounter=0;
                    adapter.notifyDataSetChanged();
                    adapter.listApproveReject.clear();
                    onRefresh();
                    checkAll.setChecked(false);
                }
            }, 1000);
        }

    }

    private void postApproveRejectIncrement(boolean succcess, @Nullable ApproveRejectToyotaDevPojo resource, int i){

        if (succcess){
            ++successCounter;
//            adapter.listApproveReject.remove(0);
            if (i < adapter.getItemCount()) {
                adapter.removeValues(i);
                adapter.notifyDataSetChanged();
            }
        }
        else{
            ++failureCounter;
            Toast.makeText(getApplicationContext(), resource.message+"-", Toast.LENGTH_LONG).show();
        }

    }

    private void getAllDraftNumber(){
        if (checkAll.isChecked()){
            Call<AllDraftNumberToyotaDevPojo> call = travelApiInterface.getAllDraftNumber("09105014", "settlement");
            call.enqueue(new Callback<AllDraftNumberToyotaDevPojo>() {
                @Override
                public void onResponse(@NonNull Call<AllDraftNumberToyotaDevPojo> call, @NonNull Response<AllDraftNumberToyotaDevPojo> response) {

                    AllDraftNumberToyotaDevPojo resource = response.body();

                    if (resource != null && resource.success){
                        adapter.listApproveReject.clear();
                        for (int i=0; i<resource.draftNumbers.size(); i++){
                            adapter.listApproveReject.add(new MyTravelListApproveRejectDataModel(Integer.toString(i), resource.draftNumbers.get(i).approveRejectDraftNumber));
                        }
                    }
                }

                @Override
                public void onFailure(@NonNull Call<AllDraftNumberToyotaDevPojo> call, @NonNull Throwable t) {
                    call.cancel();
                }
            });
        }
        else{
            adapter.listApproveReject.clear();
        }

    }

    private void refreshData(){
        adapter.listApproveReject.clear();
        listSettlementRecyclerView.invalidate();
        if (adapter.getItemCount()>0){
            adapter.clear();
        }
        listSettlementRecyclerView.setAdapter(adapter);
        listSettlementRecyclerView.showProgressPagination();
        initData("on refresh", 0);
    }

    //Override view hanlder

    @Override
    public void onClick(View v) {
        int id = v.getId();

        if (id == R.id.checkbox_select_all) {
            adapter.setAllChecked(checkAll.isChecked());
            getAllDraftNumber();
        } else if (id == -1) {//on toolbar back navigation pressed
            if (!searchView.isIconified()) {
                searchView.setIconified(true);
                listSettlementRecyclerView.showProgressPagination();
            } else {
                onBackPressed();
            }
        } else if (id == R.id.button_call_to_action_approve) {
            if (adapter.listApproveReject.size() > 0) {
                new SweetAlertDialog(this, SweetAlertDialog.WARNING_TYPE)
                        .setTitleText("Are you sure?")
                        .setContentText("You are about to approve all (" + adapter.listApproveReject.size() + ") requests.\nApproved request can't be changed later!")
                        .setConfirmText("Approve")
                        .setConfirmButtonBackgroundColor(getResources().getColor(R.color.primary_button))
                        .setConfirmClickListener(new SweetAlertDialog.OnSweetClickListener() {
                            @Override
                            public void onClick(SweetAlertDialog sDialog) {
                                sDialog.getProgressHelper().setBarColor(Color.parseColor("#A5DC86"));
                                sDialog.setTitleText("Processing...");
                                sDialog.setContentText("");
                                sDialog.setCancelable(false);
                                sDialog.showCancelButton(false);
                                sDialog.changeAlertType(SweetAlertDialog.PROGRESS_TYPE);
                                sDialog.show();
                                approveRejectRequest("settlement", "approve", sDialog, 0);
                            }
                        })
                        .setCancelButtonBackgroundColor(getResources().getColor(R.color.colorPrimary))
                        .setCancelButton("Cancel", new SweetAlertDialog.OnSweetClickListener() {
                            @Override
                            public void onClick(SweetAlertDialog sDialog) {
                                sDialog.setTitleText("Cancelled")
                                        .setConfirmText("Back")
                                        .setContentText("")
                                        .setConfirmButtonBackgroundColor(getResources().getColor(R.color.colorPrimary))
                                        .showCancelButton(false)
                                        .setCancelClickListener(null)
                                        .setConfirmClickListener(null)
                                        .changeAlertType(SweetAlertDialog.NORMAL_TYPE);
                            }
                        })
                        .show();
            }
        } else if (id == R.id.button_call_to_action_reject) {
            if (adapter.listApproveReject.size() > 0) {
                new SweetAlertDialog(this, SweetAlertDialog.WARNING_TYPE)
                        .setTitleText("Are you sure?")
                        .setContentText("You are about to reject all (" + adapter.listApproveReject.size() + ") requests.\nRejected request can't be changed later!")
                        .setConfirmText("Reject")
                        .setConfirmButtonBackgroundColor(getResources().getColor(R.color.colorPrimary))
                        .setConfirmClickListener(new SweetAlertDialog.OnSweetClickListener() {
                            @Override
                            public void onClick(SweetAlertDialog sDialog) {
                                sDialog.getProgressHelper().setBarColor(Color.parseColor("#A5DC86"));
                                sDialog.setTitleText("Processing...");
                                sDialog.setContentText("");
                                sDialog.setCancelable(false);
                                sDialog.showCancelButton(false);
                                sDialog.changeAlertType(SweetAlertDialog.PROGRESS_TYPE);
                                sDialog.show();
                                approveRejectRequest("settlement", "reject", sDialog, 0);
                            }
                        })
                        .setCancelButtonBackgroundColor(getResources().getColor(R.color.primary_button))
                        .setCancelButton("Cancel", new SweetAlertDialog.OnSweetClickListener() {
                            @Override
                            public void onClick(SweetAlertDialog sDialog) {
                                sDialog.setTitleText("Cancelled")
                                        .setConfirmText("Back")
                                        .setContentText("")
                                        .setConfirmButtonBackgroundColor(getResources().getColor(R.color.colorPrimary))
                                        .showCancelButton(false)
                                        .setCancelClickListener(null)
                                        .setConfirmClickListener(null)
                                        .changeAlertType(SweetAlertDialog.NORMAL_TYPE);
                            }
                        })
                        .show();
            }
        }
    }

    @Override
    public void onBackPressed() {
        if (!searchView.isIconified()){
            searchView.setIconified(true);
            listSettlementRecyclerView.showProgressPagination();
        }
        else{
            super.onBackPressed();
        }
    }

    @Override
    public void onRefresh() {
        if (searchView.isIconified()){
            refreshData();
        }
        else{
            searchView.setIconified(true);
            listSettlementRecyclerView.showProgressPagination();
            swipe.setRefreshing(false);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        MenuItem item = menu.findItem(R.id.action_search);
        searchView = (SearchView) item.getActionView();

        ImageView closeBtn = searchView.findViewById(R.id.search_close_btn);
        closeBtn.setImageResource(R.drawable.ic_close_white_24dp);
        closeBtn.setEnabled(false);
        closeBtn.setImageDrawable(null);
        ImageView searchBtn = searchView.findViewById(R.id.search_button);
        searchBtn.setImageResource(R.drawable.ic_search_white_24dp);

        searchView.setQueryHint(getString(R.string.type_name));
        searchView.setIconifiedByDefault(true);
        searchView.setOnQueryTextListener(this);

        return true;
    }

    //Search filter handler

    @Override
    public boolean onQueryTextSubmit(String query) {
        listSettlementRecyclerView.hidePagination();
        adapter.getFilter().filter(query);
        return false;
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        listSettlementRecyclerView.hidePagination();
        adapter.getFilter().filter(newText);
        return false;
    }

    //public function accessible from adapter

    public void setAllChecked(boolean checked){
        checkAll.setChecked(checked);
    }

//    public boolean isChecekedAll(){
//        return checkAll.isChecked();
//    }

    public void startIntentFromAdapter (Class target, MyTravelMainDataDataModel dataItem, int resultCode){
        Intent requestDetails = new Intent(this, target);
        requestDetails.putExtra(TAG_OPEN_SETTLEMENT_APPROVER_REQUEST_DETAILS_ACTIVITY, dataItem);
        startActivityForResult(requestDetails, resultCode);
    }

    //should be always at the bottom, on activity result

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode){
            case SETTLEMENT_APPROVER_REQUEST_DETAILS_RESULT_CODE:
                if (resultCode == RESULT_CANCELED){
                }
                break;
            default:
                break;
        }
    }
}
