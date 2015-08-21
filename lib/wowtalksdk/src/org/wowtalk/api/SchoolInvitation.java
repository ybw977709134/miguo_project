package org.wowtalk.api;

import java.util.Arrays;

/**
 * Created by panzy on 8/21/15.
 */
public class SchoolInvitation {
    public int id;
    public String phone;
    public String schoolId;
    public String schoolName;
    /**
     * possible values:
     * null / "sent" / "accepted" / "rejected"
     */
    public String status;
    public String[] classroomIds;
    public String[] classroomNames;

    @Override
    public String toString() {
        return "SchoolInvitation{" +
                "schoolName='" + schoolName + '\'' +
                ", classroomNames=" + Arrays.toString(classroomNames) +
                '}';
    }
}
