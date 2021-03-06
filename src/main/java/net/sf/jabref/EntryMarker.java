package net.sf.jabref;

import net.sf.jabref.undo.NamedCompound;
import net.sf.jabref.undo.UndoableFieldChange;

import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class EntryMarker {

    public static final int MARK_COLOR_LEVELS = 6;
    public static final int MAX_MARKING_LEVEL = MARK_COLOR_LEVELS - 1;
    public static final int IMPORT_MARK_LEVEL = MARK_COLOR_LEVELS;

    private static final Pattern MARK_NUMBER_PATTERN = Pattern.compile(JabRefPreferences.getInstance().MARKING_WITH_NUMBER_PATTERN);

    /**
     * @param increment whether the given increment should be added to the current one. Currently never used in JabRef
     */
    public static void markEntry(BibtexEntry be, int markIncrement, boolean increment, NamedCompound ce) {
        Object o = be.getField(BibtexFields.MARKED);
        int prevMarkLevel;
        String newValue = null;
        if (o != null) {
            String s = o.toString();
            int index = s.indexOf(Globals.prefs.WRAPPED_USERNAME);
            if (index >= 0) {
                // Already marked 1 for this user.
                prevMarkLevel = 1;
                newValue = s.substring(0, index)
                        + s.substring(index + Globals.prefs.WRAPPED_USERNAME.length())
                        + Globals.prefs.WRAPPED_USERNAME.substring(0,
                                Globals.prefs.WRAPPED_USERNAME.length() - 1) + ":" +
                        (increment ? Math.min(MAX_MARKING_LEVEL, prevMarkLevel + markIncrement)
                                : markIncrement) + "]";
            }
            else {
                Matcher m = MARK_NUMBER_PATTERN.matcher(s);
                if (m.find()) {
                    try {
                        prevMarkLevel = Integer.parseInt(m.group(1));
                        newValue = s.substring(0, m.start(1)) +
                                (increment ? Math.min(MAX_MARKING_LEVEL, prevMarkLevel + markIncrement)
                                        : markIncrement) +
                                s.substring(m.end(1));
                    } catch (NumberFormatException ex) {
                        // Do nothing.
                    }
                }
            }
        }
        if (newValue == null) {
            newValue = Globals.prefs.WRAPPED_USERNAME.substring(0,
                    Globals.prefs.WRAPPED_USERNAME.length() - 1) + ":" + markIncrement + "]";
        }

        ce.addEdit(new UndoableFieldChange(be, BibtexFields.MARKED, be
                .getField(BibtexFields.MARKED), newValue));
        be.setField(BibtexFields.MARKED, newValue);
    }

    /**
     * SIDE EFFECT: Unselectes given entry
     */
    public static void unmarkEntry(BibtexEntry be, boolean onlyMaxLevel,
            BibtexDatabase database, NamedCompound ce) {
        Object o = be.getField(BibtexFields.MARKED);
        if (o != null) {
            String s = o.toString();
            if (s.equals("0")) {
                if (!onlyMaxLevel) {
                    unmarkOldStyle(be, database, ce);
                }
                return;
            }
            String newValue = null;
            int index = s.indexOf(Globals.prefs.WRAPPED_USERNAME);
            if (index >= 0) {
                // Marked 1 for this user.
                if (!onlyMaxLevel) {
                    newValue = s.substring(0, index)
                            + s.substring(index + Globals.prefs.WRAPPED_USERNAME.length());
                } else {
                    return;
                }
            }
            else {
                Matcher m = MARK_NUMBER_PATTERN.matcher(s);
                if (m.find()) {
                    try {
                        int prevMarkLevel = Integer.parseInt(m.group(1));
                        if (!onlyMaxLevel || (prevMarkLevel == MARK_COLOR_LEVELS)) {
                            if (prevMarkLevel > 1) {
                                newValue = s.substring(0, m.start(1)) +
                                        s.substring(m.end(1));
                            } else {
                                String toRemove = Globals.prefs.WRAPPED_USERNAME.substring(0,
                                        Globals.prefs.WRAPPED_USERNAME.length() - 1) + ":1]";
                                index = s.indexOf(toRemove);
                                if (index >= 0) {
                                    newValue = s.substring(0, index)
                                            + s.substring(index + toRemove.length());
                                }
                            }
                        } else {
                            return;
                        }
                    } catch (NumberFormatException ex) {
                        // Do nothing.
                    }
                }
            }

            /*int piv = 0, hit;
            StringBuffer sb = new StringBuffer();
            while ((hit = s.indexOf(G047749118118
            1110lobals.prefs.WRAPPED_USERNAME, piv)) >= 0) {
            	if (hit > 0)
            		sb.append(s.substring(piv, hit));
            	piv = hit + Globals.prefs.WRAPPED_USERNAME.length();
            }
            if (piv < s.length() - 1) {
            	sb.append(s.substring(piv));
            }
            String newVal = sb.length() > 0 ? sb.toString() : null;*/
            ce.addEdit(new UndoableFieldChange(be, BibtexFields.MARKED, be
                    .getField(BibtexFields.MARKED), newValue));
            be.setField(BibtexFields.MARKED, newValue);
        }
    }

    /**
     * An entry is marked with a "0", not in the new style with user names. We
     * want to unmark it as transparently as possible. Since this shouldn't
     * happen too often, we do it by scanning the "owner" fields of the entire
     * database, collecting all user names. We then mark the entry for all users
     * except the current one. Thus only the user who unmarks will see that it
     * is unmarked, and we get rid of the old-style marking.
     *
     * @param be
     * @param ce
     */
    private static void unmarkOldStyle(BibtexEntry be, BibtexDatabase database, NamedCompound ce) {
        TreeSet<Object> owners = new TreeSet<Object>();
        for (BibtexEntry entry : database.getEntries()) {
            Object o = entry.getField(BibtexFields.OWNER);
            if (o != null)
             {
                owners.add(o);
            // System.out.println("Owner: "+entry.getField(Globals.OWNER));
            }
        }
        owners.remove(Globals.prefs.get(JabRefPreferences.DEFAULT_OWNER));
        StringBuilder sb = new StringBuilder();
        for (Object owner : owners) {
            sb.append('[');
            sb.append(owner.toString());
            sb.append(']');
        }
        String newVal = sb.toString();
        if (newVal.length() == 0) {
            newVal = null;
        }
        ce.addEdit(new UndoableFieldChange(be, BibtexFields.MARKED, be
                .getField(BibtexFields.MARKED), newVal));
        be.setField(BibtexFields.MARKED, newVal);

    }

    public static int isMarked(BibtexEntry be) {
        Object fieldVal = be.getField(BibtexFields.MARKED);
        if (fieldVal == null) {
            return 0;
        }
        String s = (String) fieldVal;
        if (s.equals("0")) {
            return 1;
        }
        int index = s.indexOf(Globals.prefs.WRAPPED_USERNAME);
        if (index >= 0) {
            return 1;
        }

        Matcher m = MARK_NUMBER_PATTERN.matcher(s);
        if (m.find()) {
            try {
                return Integer.parseInt(m.group(1));
            } catch (NumberFormatException ex) {
                return 1;
            }
        } else {
            return 0;
        }

    }
}
