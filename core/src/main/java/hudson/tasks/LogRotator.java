package hudson.tasks;

import hudson.model.Describable;
import hudson.model.Descriptor;
import hudson.model.Job;
import hudson.model.Run;
import hudson.scm.SCM;

import org.kohsuke.stapler.DataBoundConstructor;

import java.io.IOException;
import java.util.Calendar;
import java.util.GregorianCalendar;

/**
 * Deletes old log files.
 *
 * TODO: is there any other task that follows the same pattern?
 * try to generalize this just like {@link SCM} or {@link BuildStep}.
 *
 * @author Kohsuke Kawaguchi
 */
public class LogRotator implements Describable<LogRotator> {

    /**
     * If not -1, history is only kept up to this days.
     */
    private final int daysToKeep;

    /**
     * If not -1, only this number of build logs are kept.
     */
    private final int numToKeep;
    
    @DataBoundConstructor
    public LogRotator (String logrotate_days, String logrotate_nums) {
        this (parse(logrotate_days),parse(logrotate_nums));     	
    }

    public static int parse(String p) {
        if(p==null)     return -1;
        try {
            return Integer.parseInt(p);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    
    public LogRotator(int daysToKeep, int numToKeep) {
        this.daysToKeep = daysToKeep;
        this.numToKeep = numToKeep;
    }

    public void perform(Job<?,?> job) throws IOException {
        // keep the last successful build regardless of the status
        Run lsb = job.getLastSuccessfulBuild();

        if(numToKeep!=-1) {
            Run[] builds = job.getBuilds().toArray(new Run[0]);
            for( int i=numToKeep; i<builds.length; i++ ) {
                if(!builds[i].isKeepLog() && builds[i]!=lsb)
                    builds[i].delete();
            }
        }

        if(daysToKeep!=-1) {
            Calendar cal = new GregorianCalendar();
            cal.add(Calendar.DAY_OF_YEAR,-daysToKeep);
            // copy it to the array because we'll be deleting builds as we go.
            for( Run r : job.getBuilds().toArray(new Run[0]) ) {
                if(r.getTimestamp().before(cal) && !r.isKeepLog() && r!=lsb)
                    r.delete();
            }
        }
    }

    public int getDaysToKeep() {
        return daysToKeep;
    }

    public int getNumToKeep() {
        return numToKeep;
    }

    public String getDaysToKeepStr() {
        if(daysToKeep==-1)  return "";
        else                return String.valueOf(daysToKeep);
    }

    public String getNumToKeepStr() {
        if(numToKeep==-1)   return "";
        else                return String.valueOf(numToKeep);
    }

    public LRDescriptor getDescriptor() {
        return DESCRIPTOR;
    }

    public static final LRDescriptor DESCRIPTOR = new LRDescriptor();

    public static final class LRDescriptor extends Descriptor<LogRotator> {
        public String getDisplayName() {
            return "Log Rotation";
        }        
    }
}
