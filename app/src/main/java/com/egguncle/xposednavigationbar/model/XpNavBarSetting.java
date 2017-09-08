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

package com.egguncle.xposednavigationbar.model;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.List;

/**
 * Created by egguncle on 17-8-10.
 * 因为在Nougat上无法正常使用全局读写权限的sharedpreference,
 * 所以这里做一个妥协，将所有设置保存在一个类中，仍然使用json存储在sp中，
 * 在Nougat上虽然无法直接获取sp内容来进行扩展状态的初始化，但是可以在使用中进行进程间通信来设置扩展功能
 */

public class XpNavBarSetting implements Parcelable {
    //快捷设置的内容数据
    private List<ShortCut> mShortCutData;

    //home界面用来呼出的小点的位置
    private int mHomePointPosition;

    //小点对应的位置 左 右 不显示
    public final static int LEFT = 0;
    public final static int RIGHT = 1;
    public final static int DISMISS = 2;

    //图标缩放比例
    private int mIconSize;

    //是否使用root下拉
    private static boolean mRootDown;

    //清理内存等级
    private int mClearMenLevel;


    public XpNavBarSetting(List<ShortCut> shortCutData, int homePointPosition, int iconSize, boolean rootDown, int clearMenLevel) {
        mShortCutData = shortCutData;
        mHomePointPosition = homePointPosition;
        mIconSize = iconSize;
        mRootDown = rootDown;
        mClearMenLevel = clearMenLevel;
    }


    protected XpNavBarSetting(Parcel in) {
        mShortCutData = in.createTypedArrayList(ShortCut.CREATOR);
        mHomePointPosition = in.readInt();
        mIconSize = in.readInt();
        mRootDown = in.readByte() != 0;
        mClearMenLevel = in.readInt();
    }

    public static final Creator<XpNavBarSetting> CREATOR = new Creator<XpNavBarSetting>() {
        @Override
        public XpNavBarSetting createFromParcel(Parcel in) {
            return new XpNavBarSetting(in);
        }

        @Override
        public XpNavBarSetting[] newArray(int size) {
            return new XpNavBarSetting[size];
        }
    };

    public List<ShortCut> getShortCutData() {
        return mShortCutData;
    }

    public int getHomePointPosition() {
        return mHomePointPosition;
    }


    public int getIconSize() {
        return mIconSize;
    }

    public static boolean isRootDown() {
        return mRootDown;
    }

    public int getClearMenLevel() {
        return mClearMenLevel;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeTypedList(mShortCutData);
        dest.writeInt(mHomePointPosition);
        dest.writeInt(mIconSize);
        dest.writeByte((byte) (mRootDown ? 1 : 0));
        dest.writeInt(mClearMenLevel);
    }
}
