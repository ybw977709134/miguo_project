package co.onemeter.oneapp.ui;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.wowtalk.api.Buddy;
import org.wowtalk.api.Database;
import org.wowtalk.api.GroupMember;
import org.wowtalk.api.PrefUtil;
import org.wowtalk.ui.MessageDialog;

import java.util.ArrayList;
import java.util.List;

import co.onemeter.oneapp.R;

/**
 * teacher list of a class
 */
public class TeacherInClassActivity extends Activity implements View.OnClickListener, AdapterView.OnItemClickListener {
    public static final String INTENT_PATH = "path";
    public static final String INTENT_CLASSID = "classid";

    private String[] photopath;

    private ArrayList<GroupMember> teachers = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_teahcers_in_class);
        Intent intent = getIntent();
        String classId = intent.getStringExtra(INTENT_CLASSID);
        photopath = intent.getStringArrayExtra(INTENT_PATH);

        getLocalTeachers(classId);
        initView();
    }

    void getLocalTeachers(String classId){
        List<GroupMember> members = new Database(this).fetchGroupMembers(classId);
        for (GroupMember member : members){
            if(member.getAccountType() == Buddy.ACCOUNT_TYPE_TEACHER){
                teachers.add(member);
            }
        }
    }

    void initView(){
        findViewById(R.id.title_back).setOnClickListener(this);
        ListView lv = (ListView) findViewById(R.id.lv_teacher_in_class);
        lv.setAdapter(new TeahcerListAdapter());
        lv.setOnItemClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.title_back:
                finish();
            break;
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        String uid = teachers.get(position).userID;
        if(uid.equals(PrefUtil.getInstance(this).getUid())){
            Toast.makeText(this, "请不要发给自己!", Toast.LENGTH_LONG).show();
            return;
        }
        sureSendToteacher(uid);
    }

    private void sureSendToteacher(final String uid){
        MessageDialog dialog = new MessageDialog(this);
        dialog.setMessage("是否发送！");
        dialog.setOnLeftClickListener("确定", new MessageDialog.MessageDialogClickListener() {
            @Override
            public void onclick(MessageDialog dialog) {
                dialog.dismiss();
                MessageComposerActivity.launchToChatWithBuddyWithPicture(TeacherInClassActivity.this,uid,photopath,true);
            }
        });
        dialog.show();
    }

    private class TeahcerListAdapter extends BaseAdapter{

        @Override
        public int getCount() {
            if(teachers.isEmpty() || teachers== null){
                return 0;
            }
            return teachers.size();
        }

        @Override
        public Object getItem(int position) {
            return teachers.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder holder = null;
            if(convertView == null){
                holder = new ViewHolder();
                convertView = getLayoutInflater().inflate(R.layout.listitem_contact,parent,false);
                holder.txt_name = (TextView) convertView.findViewById(R.id.contact_name);
                holder.txt_state = (TextView) convertView.findViewById(R.id.contact_state);
                holder.img_photo = (ImageView) convertView.findViewById(R.id.contact_photo);
                holder.img_tag = (ImageView) convertView.findViewById(R.id.imageView_tag_stu_tea);
                convertView.setTag(holder);
            }else {
                holder = (ViewHolder) convertView.getTag();
            }
            holder.img_tag.setVisibility(View.VISIBLE);
            holder.txt_state.setVisibility(View.GONE);
            holder.txt_name.setGravity(Gravity.CENTER_VERTICAL);
            GroupMember teacher = teachers.get(position);
            PhotoDisplayHelper.displayPhoto(TeacherInClassActivity.this, holder.img_photo, R.drawable.default_avatar_90, teacher, true);
            holder.txt_name.setText(TextUtils.isEmpty(teacher.alias) ? teacher.nickName : teacher.alias);
            return convertView;
        }

        class ViewHolder{
            ImageView img_photo;
            TextView txt_name;
            TextView txt_state;
            ImageView img_tag;
        }
    }
}
