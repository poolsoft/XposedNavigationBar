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

package com.egguncle.xposednavigationbar.hook.hookutil;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.graphics.Color;
import android.os.Build;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.egguncle.xposednavigationbar.constant.ConstantStr;
import com.egguncle.xposednavigationbar.constant.XpNavBarAction;
import com.egguncle.xposednavigationbar.hook.btnFunc.MusicControllerPanel;
import com.egguncle.xposednavigationbar.hook.util.BtnFuncFactory;
import com.egguncle.xposednavigationbar.hook.util.MyClipBoard;
import com.egguncle.xposednavigationbar.hook.util.XpLog;
import com.egguncle.xposednavigationbar.model.ShortCut;
import com.egguncle.xposednavigationbar.model.XpNavBarSetting;
import com.egguncle.xposednavigationbar.util.ImageUtil;
import com.egguncle.xposednavigationbar.util.SPUtil;

import java.util.ArrayList;
import java.util.List;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;

/**
 * Created by egguncle on 17-8-14.
 */

public class NavBarHook {
    private static BtnFuncFactory btnFuncFactory;
    private static LinearLayout llUnderMainNavBar;
    private static boolean expandStatusBarWithRoot;
    private static LinearLayout llExtPage;
    private static MusicControllerPanel musicPanel;

    public static void hook(ClassLoader classLoader) throws Throwable {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.M) {
            XpLog.i("hook on Marshmallow");
            hookNavBarBeforeNougat(classLoader);
        } else {
            XpLog.i("hook on Nougat");
            hookNavBarOnNougat(classLoader);
        }
    }

    /**
     * hook android 7.0的导航栏
     * 在lineage os上hook资源文件的方法没生效，只能在这里做hook了
     */
    private static void hookNavBarOnNougat(ClassLoader classLoader) throws Throwable {
        final Class<?> navigationBarInflaterViewClass = classLoader.loadClass("com.android.systemui.statusbar.phone.NavigationBarInflaterView");
        XposedHelpers.findAndHookMethod(navigationBarInflaterViewClass, "onFinishInflate", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                super.afterHookedMethod(param);
                FrameLayout navbar = (FrameLayout) XposedHelpers.getObjectField(param.thisObject, "mRot0");
                FrameLayout navbarBtns = (FrameLayout) navbar.getChildAt(0);
                hookNavBarFunc(navbar, navbarBtns);
            }
        });
    }

    /**
     * hook android 7.0以下的导航栏
     *
     * @throws Throwable
     */
    private static void hookNavBarBeforeNougat(ClassLoader classLoader) throws Throwable {
        final Class<?> navigationBarInflaterViewClass = classLoader.loadClass("com.android.systemui.statusbar.phone.NavigationBarView");
        XposedHelpers.findAndHookMethod(navigationBarInflaterViewClass, "onFinishInflate", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                super.afterHookedMethod(param);
                View navBarView = (View) param.thisObject;
                View[] mRotatedViews = (View[]) XposedHelpers.getObjectField(navBarView, "mRotatedViews");
                Resources res = navBarView.getResources();
                ViewGroup rootView = (ViewGroup) mRotatedViews[Surface.ROTATION_0];
                ViewGroup navBtnsRot0 = (ViewGroup) mRotatedViews[Surface.ROTATION_0]
                        .findViewById(res.getIdentifier("nav_buttons", "id", "com.android.systemui"));
                hookNavBarFunc(rootView, navBtnsRot0);
            }
        });
    }

    /**
     * 基础的hook方法
     *
     * @param rootView
     * @param navbarView
     */
    private static void hookNavBarFunc(ViewGroup rootView, final ViewGroup navbarView) {
        Context context = rootView.getContext();

        //初始化剪贴板监听
        MyClipBoard.startListenClipboard(context);
        //初始化广播接收器
        initBroadcast(context);

        LinearLayout parentView = new LinearLayout(context);
        //加入一个viewpager，第一页为空，是导航栏本身的功能
        final ViewPager vpXphook = new ViewPager(context);

        parentView.addView(vpXphook);

        //初始化左边的整个音乐面板
        musicPanel = new MusicControllerPanel(context);
        musicPanel.setData(DataHook.mapImgRes, DataHook.iconScale);
        musicPanel.initPanel();

        initHomePoint(context, vpXphook);


        //viewpage的第二页
        llExtPage = new LinearLayout(context);
        llExtPage.setOrientation(LinearLayout.HORIZONTAL);
        llExtPage.setGravity(Gravity.CENTER_VERTICAL);

        btnFuncFactory = new BtnFuncFactory(vpXphook, llExtPage, DataHook.mapImgRes);
        for (ShortCut sc : DataHook.shortCutList) {
            btnFuncFactory.createBtnAndSetFunc(context, llExtPage, sc, DataHook.iconScale);
        }
        //将这些布局都添加到viewpageadapter中
        final List<View> list = new ArrayList<View>();
        list.add(musicPanel);
        list.add(llUnderMainNavBar);
        list.add(llExtPage);

        PagerAdapter pagerAdapter = new PagerAdapter() {
            @Override
            public int getCount() {
                return list.size();
            }

            @Override
            public boolean isViewFromObject(View view, Object object) {
                return view == object;
            }

            @Override
            public Object instantiateItem(ViewGroup container, int position) {
                container.addView(list.get(position));
                return list.get(position);
            }

            @Override
            public void destroyItem(ViewGroup container, int position, Object object) {
                container.removeView(list.get(position));
            }
        };
        vpXphook.setAdapter(pagerAdapter);
        vpXphook.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                if (position == 1) {
                    //当移动到第一页的时候，显示出导航栏，上升动画
                    XpLog.i("apper NavigationBar");
                    navbarView.setVisibility(View.VISIBLE);
                } else {
                    //当移动到非第一页的时候，隐藏导航栏本身的功能，来实现自己的一些功能。
                    XpLog.i("hide NavigationBar");
                    navbarView.setVisibility(View.GONE);
                    if (llExtPage.getChildCount() == 0) {
                        vpXphook.setCurrentItem(1);
                    }
                }
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });
        vpXphook.setCurrentItem(1);
        ViewGroup.LayoutParams params = new ViewGroup.LayoutParams(
                ViewPager.LayoutParams.MATCH_PARENT, ViewPager.LayoutParams.MATCH_PARENT);
        rootView.addView(parentView, 0, params);
    }

    /**
     * 初始化主导航栏上面的小点
     */
    private static void initHomePoint(Context context, final ViewPager vpXphook) {
        //第一个界面，与原本的导航栏重合，实际在导航栏的下层
        llUnderMainNavBar = new LinearLayout(context);
        //用于呼出整个扩展导航栏的一个小点
        ImageButton btnCall = new ImageButton(context);
        btnCall.setImageBitmap(ImageUtil.byte2Bitmap(DataHook.mapImgRes.get(ConstantStr.FUNC_SMALL_POINT_CODE)));
        btnCall.setScaleType(ImageView.ScaleType.FIT_CENTER);
        btnCall.setBackgroundColor(Color.alpha(255));
        LinearLayout.LayoutParams line1Params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        llUnderMainNavBar.addView(btnCall, line1Params);
        setHomePointPosition(DataHook.homePointPosition);

        //点击这个按钮，跳转到扩展部分
        btnCall.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                vpXphook.setCurrentItem(2);
            }
        });

    }

    public static void setHomePointPosition(int position) {
        ImageButton pointbtn = (ImageButton) llUnderMainNavBar.getChildAt(0);
        if (position == SPUtil.LEFT) {
            llUnderMainNavBar.setGravity(Gravity.LEFT);
        } else if (position == SPUtil.RIGHT) {
            llUnderMainNavBar.setGravity(Gravity.RIGHT);
        } else if (position == SPUtil.DISMISS) {
            pointbtn.setVisibility(View.GONE);
        }
    }


    /**
     * 初始化广播，用于进程间通信
     *
     * @param context
     */
    private static void initBroadcast(Context context) {
        IntentFilter dataFilter = new IntentFilter();
        dataFilter.addAction(XpNavBarAction.ACT_NAV_BAR_DATA);
        NavbarDataReceiver navbarDataReceiver = new NavBarHook.NavbarDataReceiver();
        context.registerReceiver(navbarDataReceiver, dataFilter);
    }

    private static class NavbarDataReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            //在更新以后有些用户没有进行重启，而直接进行了新功能的添加，app部分得到了更新，
            //但是hook部分的更新需要重启后才生效，所以这里做一个异常捕获操作
            try {
                XpNavBarSetting setting = intent.getParcelableExtra("data");
                xpNavBarDataAnalysis(context, setting);
            } catch (Exception e) {
                XpLog.e(e);
            }
        }
    }

    /**
     * 解析xpnvbarsetting的内容
     *
     * @param context
     * @param setting
     */
    public static void xpNavBarDataAnalysis(Context context, XpNavBarSetting setting) {
        List<ShortCut> list = setting.getShortCutData();
        int iconSize = setting.getIconSize();
        int homePosition = setting.getHomePointPosition();
        boolean rootDown = setting.isRootDown();
        int clearMemLevel = setting.getClearMenLevel();
        boolean chameleonNavbar = setting.isChameleonNavbar();
        int navbarHeight = setting.getNavbarHeight();
        updateNavBar(context, list, homePosition, iconSize,
                rootDown, clearMemLevel, chameleonNavbar, navbarHeight);
    }

    /**
     * 根据获取到的数据来更新导航栏
     *
     * @param shortCutData
     * @param homePointPosition
     * @param iconSize
     * @param rootDown
     */
    public static void updateNavBar(Context context,
                                    List<ShortCut> shortCutData,
                                    int homePointPosition,
                                    int iconSize,
                                    boolean rootDown,
                                    int clearMemLevel,
                                    boolean chameleonNavbar,
                                    int navbarHeight) {
        btnFuncFactory.clearAllBtn();
        if (shortCutData != null && shortCutData.size() != 0) {
            DataHook.shortCutList = shortCutData;
            for (ShortCut sc : shortCutData) {
                btnFuncFactory.createBtnAndSetFunc(context, llExtPage, sc, iconSize);
            }
        }
        musicPanel.updateIconSize(iconSize);
        setHomePointPosition(homePointPosition);
        expandStatusBarWithRoot = rootDown;
        DataHook.clearMenLevel = clearMemLevel;
        DataHook.chameleonNavbar = chameleonNavbar;
        DataHook.navbarHeight=navbarHeight;

        Intent intent=new Intent(XpNavBarAction.ACTION_PHONE_WINDOW_MANAGER);
        intent.putExtra(ConstantStr.TYPE,ConstantStr.NAVBAR_H);
        intent.putExtra(ConstantStr.NAVBAR_HEIGHT,navbarHeight);
        context.sendBroadcast(intent);
    }


    public static boolean isExpandStatusBarWithRoot() {
        return expandStatusBarWithRoot;
    }

}
