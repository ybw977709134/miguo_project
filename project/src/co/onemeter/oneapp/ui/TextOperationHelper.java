package co.onemeter.oneapp.ui;

import org.wowtalk.api.ChatMessage;
import org.wowtalk.api.Database;

import android.content.Context;
import android.content.Intent;
import android.text.ClipboardManager;
import android.view.View;
import co.onemeter.oneapp.helper.HyperLinkHelper;
import co.onemeter.oneapp.helper.PhoneNumberHelper;
import co.onemeter.oneapp.R;

/**
 * Created with IntelliJ IDEA.
 * User: pan
 * Date: 13-6-19
 * Time: PM3:05
 * To change this template use File | Settings | File Templates.
 */
public class TextOperationHelper {

    public static void fillMenu(final Context ctx,
                                final BottomButtonBoard mMenu,
                                final ChatMessage messageDetail,
                                final String message,
                                boolean addCloseItem) {
        String[] phones = PhoneNumberHelper.extractPhoneNumbers(message);
        String[] links = HyperLinkHelper.extractHyperLinks(message);
        fillMenu(ctx, mMenu,messageDetail, message, phones, links, addCloseItem);
    }

    public static void fillMenu(final Context ctx,
                                final BottomButtonBoard mMenu,
                                final ChatMessage messageDetail,
                                final String message,
                                String[] phones,
                                String[] links,
                                boolean addCloseItem) {

        mMenu.add(ctx.getString(R.string.chat_menu_copy),
                BottomButtonBoard.BUTTON_BLUE,
                new View.OnClickListener(){
                    @Override
                    public void onClick(View view) {
                        ClipboardManager cm = (ClipboardManager)ctx.getSystemService(ctx.CLIPBOARD_SERVICE);
                        if (cm != null) {
                            cm.setText(message);
                            mMenu.dismiss();
                        }
                    }
                });
        
        //注释掉这个功能，好友圈中的删除功能在自身的类中做了判断
//        mMenu.add(ctx.getString(R.string.contacts_local_delete),
//                BottomButtonBoard.BUTTON_BLUE,
//                new View.OnClickListener(){
//                    @Override
//                    public void onClick(View view) {
//                    	Database db = new Database(ctx);
//                    	db.deleteChatMessage(messageDetail);
//                    	mMenu.dismiss();
//                    }
//                });
        
        if (phones != null && phones.length > 0) {
            for(final String phone : phones) {
                mMenu.add(ctx.getString(R.string.chat_menu_call) + " " + phone,  BottomButtonBoard.BUTTON_BLUE,
                        new View.OnClickListener(){
                            @Override
                            public void onClick(View view) {
                                Intent dial = new Intent(Intent.ACTION_DIAL, android.net.Uri.parse("tel:" + phone));
                                ctx.startActivity(dial);
                                mMenu.dismiss();
                            }
                        });
            }
        }
        if (links != null && links.length > 0) {
            for(final String link : links) {
                mMenu.add(ctx.getString(R.string.chat_menu_open_url) + " " + abbr(link, 20),
                        BottomButtonBoard.BUTTON_BLUE,
                        new View.OnClickListener(){
                            @Override
                            public void onClick(View view) {
                                Intent open = new Intent(Intent.ACTION_VIEW, android.net.Uri.parse(link));
                                ctx.startActivity(open);
                                mMenu.dismiss();
                            }
                        });
            }
        }
        if (addCloseItem) {
            mMenu.addCancelBtn(ctx.getString(R.string.close));
        }
        mMenu.show();
    }

    // 截断长字符串，追加"..."
    public static String abbr(String src, int maxlen)
    {
        if(maxlen <= 3)
            return src;

        int n = src.length();
        if(n <= maxlen)
            return src;
        else
            return src.substring(0, maxlen - 3) + "...";
    }
}
