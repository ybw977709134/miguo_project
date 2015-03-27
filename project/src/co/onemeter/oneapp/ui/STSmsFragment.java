package co.onemeter.oneapp.ui;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import co.onemeter.oneapp.R;

/**
 * Created by jacky on 15-3-27.
 */
public class STSmsFragment extends Fragment{

    private ListView listview_sms;

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View contentView = inflater.inflate(R.layout.sms_page,container,false);
        contentView.findViewById(R.id.title_bar).setVisibility(View.GONE);
        contentView.findViewById(R.id.layout_bg).setVisibility(View.GONE);
        contentView.findViewById(R.id.search_glass_img).setVisibility(View.GONE);

        listview_sms = (ListView) contentView.findViewById(R.id.sms_list);
        return contentView;
    }
}
