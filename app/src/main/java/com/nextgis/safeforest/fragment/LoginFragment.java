/*
 * Project: Forest violations
 * Purpose: Mobile application for registering facts of the forest violations.
 * Author:  Dmitry Baryshnikov (aka Bishop), bishop.dev@gmail.com
 * *****************************************************************************
 * Copyright (c) 2015-2015. NextGIS, info@nextgis.com
 *
 * This is free and unencumbered software released into the public domain.
 * 
 * Anyone is free to copy, modify, publish, use, compile, sell, or
 * distribute this software, either in source code form or as a compiled
 * binary, for any purpose, commercial or non-commercial, and by any
 * means.
 * 
 * In jurisdictions that recognize copyright laws, the author or authors
 * of this software dedicate any and all copyright interest in the
 * software to the public domain. We make this dedication for the benefit
 * of the public at large and to the detriment of our heirs and
 * successors. We intend this dedication to be an overt act of
 * relinquishment in perpetuity of all present and future rights to this
 * software under copyright law.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 * IN NO EVENT SHALL THE AUTHORS BE LIABLE FOR ANY CLAIM, DAMAGES OR
 * OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE,
 * ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 * 
 * For more information, please refer to <http://unlicense.org>
 */

package com.nextgis.safeforest.fragment;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.content.Loader;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import com.nextgis.maplib.api.IGISApplication;
import com.nextgis.maplib.datasource.GeoEnvelope;
import com.nextgis.maplibui.activity.NGWSettingsActivity;
import com.nextgis.maplibui.service.HTTPLoader;
import com.nextgis.safeforest.R;
import com.nextgis.maplibui.fragment.NGWLoginFragment;
import com.nextgis.safeforest.util.Constants;
import com.nextgis.safeforest.util.SettingsConstants;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The login fragment to the forest violations server
 */
public class LoginFragment extends NGWLoginFragment {

    protected Map<String, GeoEnvelope> mWorkingBorders;
    protected Button   mSkipButton;
    protected Spinner mRegion;

    @Override
    public View onCreateView(
            LayoutInflater inflater,
            @Nullable
            ViewGroup container,
            @Nullable
            Bundle savedInstanceState)
    {
        final View view = inflater.inflate(R.layout.fragment_login, container, false);
        mURL = (EditText) view.findViewById(R.id.url);
        mLogin = (EditText) view.findViewById(R.id.login);
        mPassword = (EditText) view.findViewById(R.id.password);
        mSignInButton = (Button) view.findViewById(R.id.signin);
        mSkipButton = (Button) view.findViewById(R.id.skip);

        TextWatcher watcher = new LocalTextWatcher();
        mURL.addTextChangedListener(watcher);
        mLogin.addTextChangedListener(watcher);
        mPassword.addTextChangedListener(watcher);

        mWorkingBorders = new HashMap<>(2);
        mWorkingBorders.put(getString(R.string.KHA), new GeoEnvelope(14538325.50, 16419624.89, 5812457.49, 8954618.39));
        mWorkingBorders.put(getString(R.string.PRI), new GeoEnvelope(14504929.65, 15506805.07, 5236173.78, 6190443.81));
        List<String> spinnerArray =  new ArrayList<>(mWorkingBorders.keySet());
        ArrayAdapter<String> adapter = new ArrayAdapter<>(getActivity(), android.R.layout.simple_spinner_item, spinnerArray);

        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mRegion = (Spinner) view.findViewById(R.id.region_select_spinner);
        mRegion.setAdapter(adapter);

        return view;
    }


    @Override
    public void onResume()
    {
        super.onResume();
        mSignInButton.setOnClickListener(this);
        mSkipButton.setOnClickListener(this);
    }


    @Override
    public void onPause()
    {
        mSignInButton.setOnClickListener(null);
        mSkipButton.setOnClickListener(null);
        super.onPause();
    }


    @Override
    public void onClick(View v)
    {
        final SharedPreferences.Editor edit =
                PreferenceManager.getDefaultSharedPreferences(getActivity()).edit();
        String region = (String) mRegion.getSelectedItem();
        GeoEnvelope env = mWorkingBorders.get(region);
        edit.putFloat(SettingsConstants.KEY_PREF_USERMINX, (float) env.getMinX());
        edit.putFloat(SettingsConstants.KEY_PREF_USERMINY, (float) env.getMinY());
        edit.putFloat(SettingsConstants.KEY_PREF_USERMAXX, (float) env.getMaxX());
        edit.putFloat(SettingsConstants.KEY_PREF_USERMAXY, (float) env.getMaxY());
        edit.commit();

        if (v == mSignInButton) {
            getLoaderManager().restartLoader(R.id.auth_token_loader, null, this);
        }
        else if (v == mSkipButton) {
            getLoaderManager().restartLoader(R.id.non_auth_token_loader, null, this);
        }
    }

    @Override
    public Loader<String> onCreateLoader(
            int id,
            Bundle args)
    {
        if (id == R.id.auth_token_loader) {
            return new HTTPLoader(
                    getActivity().getApplicationContext(), mURL.getText().toString().trim(),
                    mLogin.getText().toString(), mPassword.getText().toString());
        }
        else if (id == R.id.non_auth_token_loader) {
            return new HTTPLoader(
                    getActivity().getApplicationContext(), mURL.getText().toString().trim(),
                    null, null);
        }
        return null;
    }

    @Override
    public void onLoadFinished(
            Loader<String> loader,
            String token)
    {
        if (loader.getId() == R.id.auth_token_loader) {
            if (token != null && token.length() > 0) {
                onTokenReceived(getString(R.string.account_name), token);
            } else {
                Toast.makeText(getActivity(), R.string.error_login, Toast.LENGTH_SHORT).show();
            }
        }
        else if(loader.getId() == R.id.non_auth_token_loader){
            onTokenReceived(getString(R.string.account_name), Constants.ANONYMOUS);
        }
    }

    @Override
    protected void updateButtonState()
    {
        if (checkEditText(mURL)){
            mSkipButton.setEnabled(true);
            if( checkEditText(mLogin) && checkEditText(mPassword)) {
                mSignInButton.setEnabled(true);
            }
        }
    }

    public void onTokenReceived(
            String accountName,
            String token)
    {
        if(token.equals(Constants.ANONYMOUS)){
            IGISApplication app = (IGISApplication) getActivity().getApplication();

            if (mForNewAccount) {
                boolean accountAdded = app.addAccount(accountName, mURL.getText().toString(), Constants.ANONYMOUS, Constants.ANONYMOUS, token);
                if(accountAdded) {
                    if (null != mOnAddAccountListener) {
                        mOnAddAccountListener.onAddAccount(app.getAccount(accountName), token, accountAdded);
                    }
                }
                else {
                    if (null != mOnAddAccountListener) {
                        mOnAddAccountListener.onAddAccount(null, token, accountAdded);
                    }
                }

            } else {
                // do nothing, guest account cannot be changed
                getActivity().finish();
            }
        }
        else{
            super.onTokenReceived(accountName, token);
        }
    }
}
