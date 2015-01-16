package co.onemeter.oneapp.contacts.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import co.onemeter.oneapp.R;
import co.onemeter.oneapp.contacts.model.Person;
import co.onemeter.oneapp.contacts.util.ContactUtil;
import co.onemeter.oneapp.ui.PhotoDisplayHelper;

import org.wowtalk.api.Buddy;
import org.wowtalk.api.Database;
import org.wowtalk.api.IHasPhoto;

import java.util.*;

public class ContactListAdapter extends BaseAdapter implements Filterable {
	private Context context;
	private boolean _defaultSelected = false;
	private int _totalSelectedNumber;
	private String oldFirstChar;
	private boolean mIsShowFirstChar = true;

    // 总是以 mFilteredPersons 为有效数据源，如果没有 filter，则 mFilteredPersons == mAllPersons
	private ArrayList<Person> mAllPersons;
    private ArrayList<Person> mFilteredPersons;

    private Database mDbHelper;
    private Filter mFilter;

    public ContactListAdapter(Context context) {
		this.context = context;
		_totalSelectedNumber = 0;
		ContactUtil.fFetchNormalPersons(context);
		mFilteredPersons = mAllPersons = ContactUtil.allPersons;
        mDbHelper = new Database(context);
	}

	public ContactListAdapter(Context context,
            String[] mCurrentMemberIds) {
        this(context);
        Person tempPerson = null;
        String personId = null;
        List<String> currentIds = Arrays.asList(mCurrentMemberIds);
        for (Iterator<Person> iterator = mAllPersons.iterator(); iterator.hasNext();) {
            tempPerson = iterator.next();
            personId = tempPerson.getID();
            if (currentIds.contains(personId)){
                iterator.remove();
            }
        }
	}

	public ContactListAdapter(Context context, ArrayList<Person> allPersons) {
        this.context = context;
        _totalSelectedNumber = 0;
        mAllPersons = allPersons;
        mFilteredPersons = allPersons;
        mDbHelper = new Database(context);
    }

	public void setIsShowFirstChar (boolean isShowFirstChar) {
	    mIsShowFirstChar = isShowFirstChar;
	}

    @Override
	public int getCount() {
		return mFilteredPersons.size();
	}

	@Override
	public Person getItem(int position) {
		return mFilteredPersons.get(position);
	}

	@Override
	public long getItemId(int position) {
		return position;
	}
	
	public void setPersonSource(ArrayList<Person> persons) {
		this.mFilteredPersons = mAllPersons = persons;
	}
	
	public ArrayList<Person> getPersonSource() {
		return this.mFilteredPersons;
	}
	
	public void setDefaultSelected(boolean value) {
		_defaultSelected = value;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		ViewHolder holder = null;
        char oldFirstChar;
        char newFirstChar;
        if (position == 0) {
            oldFirstChar = 0;
        } else {
            oldFirstChar = mFilteredPersons.get(position - 1).getSortKey().toUpperCase().trim().charAt(0);
            if (oldFirstChar < 'A') {
                oldFirstChar = '#';
            }
        }
		if (convertView == null) {
			holder = new ViewHolder();
			convertView = LayoutInflater.from(context).inflate(R.layout.listitem_contact, null);
			holder.txtContactFirstChar = (TextView) convertView.findViewById(R.id.contact_first_char);
			holder.imgDivider = (ImageView) convertView.findViewById(R.id.divider_view);
			holder.imgPhoto = (ImageView) convertView.findViewById(R.id.contact_photo);
			holder.imgSelected = (ImageView) convertView.findViewById(R.id.img_selected);
			holder.txtContactName = (TextView) convertView.findViewById(R.id.contact_name);
			holder.txtContactState = (TextView) convertView.findViewById(R.id.contact_state);
			holder.isStuTea = (ImageView) convertView.findViewById(R.id.imageView_tag_stu_tea);
			convertView.setTag(holder);
		} else {
			holder = (ViewHolder) convertView.getTag();
		}
		Person person = mFilteredPersons.get(position);
		if (mIsShowFirstChar) {
		    newFirstChar = person.getSortKey().toUpperCase().trim().charAt(0);
		    if (newFirstChar < 'A')
		        newFirstChar = '#';
		    if (newFirstChar > oldFirstChar) {
//		        holder.imgDivider.setVisibility(View.GONE);
		        holder.txtContactFirstChar.setVisibility(View.VISIBLE);
		        holder.txtContactFirstChar.setText(String.valueOf(newFirstChar));
		    } else {
		        holder.txtContactFirstChar.setVisibility(View.GONE);
//		        holder.imgDivider.setVisibility(View.VISIBLE);
		    }
		} else {
		    holder.imgDivider.setVisibility(position == 0 ? View.GONE : View.VISIBLE);
        }
//        if (GlobalValue.RELEASE_AS_WOWCITY) {
//        } else {
//            holder.txtContactFirstChar.setVisibility(View.GONE);
//            holder.imgDivider.setVisibility(View.VISIBLE);
//        }

//        Buddy b = mDbHelper.buddyWithUserID(person.getID());
        IHasPhoto entity = person;
        holder.imgPhoto.setBackgroundDrawable(null);
        if (person.getAccountType() == Buddy.ACCOUNT_TYPE_PUBLIC) {
            PhotoDisplayHelper.displayPhoto(context, holder.imgPhoto,
                    R.drawable.default_official_avatar_90, entity, true);
        } else {
            PhotoDisplayHelper.displayPhoto(context, holder.imgPhoto,
                    R.drawable.default_avatar_90, entity, true);
        }

        if(person.getAccountType() == Buddy.ACCOUNT_TYPE_TEACHER){
        	holder.isStuTea.setVisibility(View.VISIBLE);
        }else {
        	holder.isStuTea.setVisibility(View.GONE);
        }
		if (_defaultSelected) {
			holder.imgSelected.setVisibility(View.VISIBLE);
			holder.imgSelected.setBackgroundResource(person.isSelected() ? R.drawable.list_selected : R.drawable.list_unselected);
		} else {
			holder.imgSelected.setVisibility(View.GONE);
		}
		holder.txtContactName.setText(person.getName());
		holder.txtContactState.setText(person.getPersonState());
		
		
		return convertView;
	}

    @Override
    public void notifyDataSetChanged() {
        // 先将联系人排序
        Collections.sort(mFilteredPersons, new Comparator<Person>() {
            @Override
            public int compare(Person lhs, Person rhs) {
                char lChar= lhs.getSortKey().toUpperCase().trim().charAt(0);
                char rChar= rhs.getSortKey().toUpperCase().trim().charAt(0);
                return lChar - rChar;
            }
        });
        super.notifyDataSetChanged();
    }

    @Override
    public Filter getFilter() {
        if (mFilter != null)
            return mFilter;

        mFilter = new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence charSequence) {
                if (charSequence == null)
                    return null;

                if (mAllPersons == null || mAllPersons.isEmpty())
                    return null;

                String fs = charSequence.toString().trim().toLowerCase();

                FilterResults fr = new FilterResults();
                ArrayList<Person> values = new ArrayList<Person>();

                for(Person p : mAllPersons) {
                    String s = p.getName();
                    if (s != null && s.toLowerCase().contains(fs)) {
                        values.add(p);
                    }
                }

                fr.values = values;
                fr.count = values.size();

                return fr;
            }

            @Override
            protected void publishResults(CharSequence charSequence, FilterResults filterResults) {
                if (null == filterResults || null == filterResults.values) {
                    // show all
                    mFilteredPersons = mAllPersons;
                } else {
                    // show filtered
                    mFilteredPersons = (ArrayList<Person>)filterResults.values;
                }
                notifyDataSetChanged();
            }
        };
        return mFilter;
    }

    static class ViewHolder {
		TextView txtContactFirstChar;
		ImageView imgDivider;
		ImageView imgSelected;
		ImageView imgPhoto;
		TextView txtContactName;
		TextView txtContactState;
		ImageView isStuTea;
	}

}
