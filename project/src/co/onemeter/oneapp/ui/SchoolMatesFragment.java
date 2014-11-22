package co.onemeter.oneapp.ui;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import co.onemeter.oneapp.R;

/**
 * <p>显示“校园里”通讯录。</p>
 * Created by pzy on 11/22/14.
 */
public class SchoolMatesFragment extends Fragment {
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_schoolmates, container, false);
    }

    public boolean handleBackPress() {
        return false;
    }

    public void gotoTop() {

    }

    public void refresh() {

    }
}