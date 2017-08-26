/*
 *     Navigation bar function expansion module
 *     Copyright (C) 2017 egguncle cicadashadow@gmail.com
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.egguncle.xposednavigationbar.ui.fragment;

import android.content.Context;
import android.support.design.widget.Snackbar;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;

import com.egguncle.xposednavigationbar.MyApplication;
import com.egguncle.xposednavigationbar.R;
import com.egguncle.xposednavigationbar.model.ShortCut;
import com.egguncle.xposednavigationbar.ui.adapter.RcvHomeAdapter;
import com.egguncle.xposednavigationbar.ui.touchHelper.MyItemTouchHelpCallBack;
import com.egguncle.xposednavigationbar.ui.touchHelper.MyItemTouchHelper;
import com.egguncle.xposednavigationbar.util.SPUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by egguncle on 17-8-11.
 */

public class SetFunFragment extends BaseFragment {
    private final static String TAG = SetFunFragment.class.getName();

    private RecyclerView rcvSetting;
    private View parentView;

    private RcvHomeAdapter rcvHomeAdapter;
    private ArrayList<ShortCut> shortCutList;

    //一个被设置和没被设置的处理逻辑

    //  private List<String> notSelectList;

    private SPUtil spUtil;
    private boolean tapsNotAppear;

    private String[] funcs;

    private Context mContext;


    public SetFunFragment() {
        mContext = MyApplication.getContext();
    }

    @Override
    void initView(View view) {
        rcvSetting = (RecyclerView) view.findViewById(R.id.rcv_setting);
        parentView = view.findViewById(R.id.parent_view);
        rcvSetting.setLayoutManager(new LinearLayoutManager(view.getContext()));
    }

    @Override
    void initAction() {

    }

    @Override
    void initVar() {

        //notSelectList = new ArrayList<>();
        shortCutList = new ArrayList<>();
        rcvHomeAdapter = new RcvHomeAdapter(mContext, shortCutList);
        rcvSetting.setAdapter(rcvHomeAdapter);
        //设置rcv可拖动
        MyItemTouchHelper myItemTouchHelper = new MyItemTouchHelper(onItemTouchCallbackListener);
        myItemTouchHelper.attachToRecyclerView(rcvSetting);

        //获取原有的设置数据
        spUtil = SPUtil.getInstance(mContext);
        List<ShortCut> list = spUtil.getAllShortCutData();
        if (list != null && list.size() != 0) {
            shortCutList.addAll(list);
            rcvHomeAdapter.notifyDataSetChanged();
        }
        funcs = getResources().getStringArray(R.array.shortcut_names);
    }

    @Override
    int getLayoutId() {
        return R.layout.f_set_func;
    }

    /**
     * 根据选择的内容，来向shortcutlist添加数据
     *
     * @param code 对应的功能码
     */
    private void addToShortCutList(int code) {
        //限制最大按钮数
        if (shortCutList.size() >= 10) {
            Snackbar.make(parentView,
                    getResources().getString(R.string.taps_too_mang_sc),
                    Snackbar.LENGTH_SHORT).show();
        } else {
            Log.i(TAG, "addToShortCutList: add ");
            ShortCut sc = new ShortCut();
            sc.setCode(code);
            shortCutList.add(sc);
        }
    }

    public void coverData(List<Integer> data) {
        //将设置的都加到select中，remove notselect中对应内容
        for (Integer code : data) {
            Log.i(TAG, "onClick: " + code);
            addToShortCutList(code);
            //   notSelectList.remove(s);
        }
        rcvHomeAdapter.notifyDataSetChanged();
    }

    public  ArrayList<ShortCut> getShortCutCodes(){
        return shortCutList;
    }

    private MyItemTouchHelpCallBack.OnItemTouchCallbackListener onItemTouchCallbackListener = new MyItemTouchHelpCallBack.OnItemTouchCallbackListener() {
        @Override
        public void onSwiped(int adapterPosition) {
            // 滑动删除的时候，从数据源移除，并刷新这个Item。
            if (shortCutList != null) {
                shortCutList.remove(adapterPosition);
                rcvHomeAdapter.notifyItemRemoved(adapterPosition);
            }
        }

        @Override
        public boolean onMove(int srcPosition, int targetPosition) {
            if (shortCutList != null) {
                // 更换数据源中的数据Item的位置
                Collections.swap(shortCutList, srcPosition, targetPosition);
                // 更新UI中的Item的位置，主要是给用户看到交互效果
                rcvHomeAdapter.notifyItemMoved(srcPosition, targetPosition);
//                for (ShortCut sc : shortCutList) {
//                    Log.i(TAG, "onMove: " + sc.getName());
//                }
                Log.i(TAG, "onMove: ---");
                return true;
            }
            return false;
        }
    };


}