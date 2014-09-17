package org.wowtalk.api;

/**
 * 控制在biz的群组listview显示时的状态，与业务无关，只是展示
 * @author zjh
 *
 */
public class TreeNode {

    /**
     * 此节点是否展示
     */
    public boolean isShow;

    /**
     * 此节点是否展开(listView)
     */
    public boolean isExpand;

    /**
     * 此节点缩进的层级
     */
    public int indentLevel;
}
