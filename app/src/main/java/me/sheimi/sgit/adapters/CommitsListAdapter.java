package me.sheimi.sgit.adapters;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import me.sheimi.android.activities.SheimiFragmentActivity;
import me.sheimi.android.utils.BasicFunctions;
import me.sheimi.sgit.R;
import me.sheimi.sgit.database.models.Repo;
import me.sheimi.sgit.repo.tasks.repo.GetCommitGraphTask;
import me.sheimi.sgit.repo.tasks.repo.GetCommitTask;
import me.sheimi.sgit.repo.tasks.repo.GetCommitTask.GetCommitCallback;

import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.revplot.PlotCommit;
import org.eclipse.jgit.revplot.PlotLane;
import org.eclipse.jgit.revwalk.RevCommit;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import androidx.core.content.ContextCompat;


/**
 * Created by sheimi on 8/18/13.
 */
public class CommitsListAdapter extends BaseAdapter {

    private Repo mRepo;
    private DateFormat mCommitDateFormatter;
    private Set<Integer> mChosenItems;
    private String mFilter;
    private ArrayList<RevCommit> mAll;
    private ArrayList<Integer> mFiltered;
    private Context mContext;
    private String mFile;
    private final ExecutorService mExecutor = Executors.newSingleThreadExecutor();
    private final Handler mMainHandler = new Handler(Looper.getMainLooper());
    private Future<?> mFilterFuture;
    private int mPosted;
    private Object mProgressLock = new Object();
    private boolean mIsIncomplete;
    private int mProgressCursor;
    private long mPostAtTime;
    /**
     * Full branch/merge topology graph mode (current vs. all branches) is only offered for
     * the main repo Commits tab (mFile == null) -- the file-scoped commit history used by
     * ViewFileActivity always uses the simpler, unchanged GetCommitTask path.
     */
    private boolean mAllBranches = false;

    /**
     * Bumped by {@link #stopFiltering()} every time the filter or commit list changes. A worker
     * (or an update it posted) from an older generation must not touch the current results:
     * cancel() only interrupts, so a superseded worker could keep adding indices to the new
     * mFiltered, and a stale postUpdate() could start a second worker for the new filter --
     * either way the same commit ended up listed twice, which crashed the Compose commit list
     * ("Key ... was already used") when searching quickly.
     */
    private int mGeneration;

    private void startFilteringWorker() {
        final int generation = mGeneration;
        final String filter = mFilter;
        final ArrayList<RevCommit> all = mAll;
        final ArrayList<Integer> filtered = mFiltered;
        final int start = mProgressCursor;
        mFilterFuture = mExecutor.submit(() -> {
            for (int i = start; i < all.size(); i++) {
                if (Thread.currentThread().isInterrupted()) {
                    return;
                }
                synchronized (mProgressLock) {
                    if (generation != mGeneration) {
                        return;
                    }
                    if (filtered.size() != mPosted && System.nanoTime() > mPostAtTime) {
                        mProgressCursor = i;
                        mPosted = filtered.size();
                        postUpdate(generation);
                        return;
                    }
                }
                if (isAccepted(all.get(i), filter)) {
                    synchronized (mProgressLock) {
                        if (generation != mGeneration) {
                            return;
                        }
                        filtered.add(i);
                    }
                }
            }
            synchronized (mProgressLock) {
                if (generation != mGeneration) {
                    return;
                }
                mPosted = filtered.size();
                mIsIncomplete = false;
            }
            postUpdate(generation);
        });
    }

    private void postUpdate(int generation) {
        mMainHandler.post(() -> {
            synchronized (mProgressLock) {
                if (generation != mGeneration) {
                    return;
                }
                notifyDataSetChanged();
                if (mIsIncomplete) {
                    // Updates after 1 s
                    mPostAtTime = System.nanoTime() + 1_000_000_000;
                    startFilteringWorker();
                }
            }
        });
    }

    public CommitsListAdapter(Context context, Set<Integer> chosenItems,
                              Repo repo, String file) {
        super();
        mFile = file;
        mContext = context;
        mChosenItems = chosenItems;
        mRepo = repo;
        mAll = new ArrayList<RevCommit>();
        mFiltered = null;
        mFilter = null;
        mCommitDateFormatter = android.text.format.DateFormat.getDateFormat(mContext);
    }

    private static boolean isAccepted(RevCommit in, String filter) {
        if (filter == null) {
            return true;
        }
        if (in.getId().toString().startsWith("commit " + filter.toLowerCase(Locale.ROOT))) {
            return true;
        }
        /* Search in raw buffer is fast but it may find the string in
         * e.g. parents field or as part of keyword. So first search in
         * raw buffer and then look in parsed components if raw buffer
         * contains needle. */
        if (!new String(in.getRawBuffer()).contains(filter)) {
            return false;
        }
        return (in.getAuthorIdent().getName().contains(filter)
                || in.getAuthorIdent().getEmailAddress().contains(filter)
                || in.getCommitterIdent().getName().contains(filter)
                || in.getCommitterIdent().getEmailAddress().contains(filter)
                || in.getFullMessage().contains(filter));
    }

    private void stopFiltering() {
        mGeneration++;
        try {
            if (mFilterFuture != null) {
                mFilterFuture.cancel(true);
                mFilterFuture = null;
            }
        } catch (Exception ignored) {
        }
    }

    private void doFiltering() {
        mFiltered = null;
        if (mFilter != null) {
            mPosted = 0;
            mIsIncomplete = true;
            mFiltered = new ArrayList<>();
            mProgressCursor = 0;
            // Show first result after 100 ms
            mPostAtTime = System.nanoTime() + 100000000;
            startFilteringWorker();
        } else {
            notifyDataSetChanged();
        }
    }

    public void setFilter(String query) {
        synchronized (mProgressLock) {
            stopFiltering();
            if (query == null || query.equals("")) {
                mFilter = null;
            } else {
                mFilter = query;
            }
            doFiltering();
        }
    }

    @Override
    public int getCount() {
        if (mFilter == null)
            return mAll.size();
        if (mIsIncomplete)
            return mPosted + 1;
        return mFiltered.size();
    }

    @Override
    public long getItemId(int position) {
        if (mIsIncomplete && position >= mPosted) {
            return -1;
        }
        if (mFilter == null) {
            return position;
        } else {
            try {
                return mFiltered.get(position);
            } catch (Exception e) {
                return -1;
            }
        }
    }

    public RevCommit getItem(int position) {
        if (mIsIncomplete && position >= mPosted) {
            return null;
        }
        try {
            return (mFilter == null) ? mAll.get(position) : mAll.get(mFiltered.get(position));
        } catch (Exception e) {
            return null;
        }
    }

    public boolean isProgressBar(int position) {
        if (mFilter == null)
            return position >= mAll.size();
        if (mIsIncomplete)
            return position >= mPosted;
        return position >= mFiltered.size();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        LayoutInflater inflater = LayoutInflater.from(mContext);
        if (isProgressBar(position)) {
            ProgressBar pb = new ProgressBar(mContext, null, android.R.attr.progressBarStyleLarge);
            return pb;
        }
        CommitsListItemHolder holder = null;
        if (convertView != null) {
            holder = (CommitsListItemHolder) convertView.getTag();
        }
        if (holder == null) {
            convertView = inflater.inflate(R.layout.listitem_commits, parent,
                    false);
            holder = new CommitsListItemHolder();
            holder.commitsTitle = (TextView) convertView
                    .findViewById(R.id.commitTitle);
            holder.commitsIcon = (ImageView) convertView
                    .findViewById(R.id.commitIcon);
            holder.commitAuthor = (TextView) convertView
                    .findViewById(R.id.commitAuthor);
            holder.commitsMsg = (TextView) convertView
                    .findViewById(R.id.commitMsg);
            holder.commitTime = (TextView) convertView
                    .findViewById(R.id.commitTime);
            convertView.setTag(holder);
        }
        RevCommit commit = getItem(position);
        PersonIdent person = commit.getAuthorIdent();
        Date date = person.getWhen();
        String email = person.getEmailAddress();

        holder.commitsTitle
                .setText(Repo.getCommitDisplayName(commit.getName()));
        holder.commitAuthor.setText(person.getName());
        holder.commitsMsg.setText(commit.getShortMessage());
        holder.commitTime.setText(mCommitDateFormatter.format(date));

        BasicFunctions.setAvatarImage(holder.commitsIcon, email);

        int color, colorResId;
        if (mChosenItems.contains(position)) {
            colorResId = R.color.pressed_sgit;
        } else {
            colorResId = android.R.color.transparent;
        }
        if (mContext instanceof SheimiFragmentActivity) {
            color = ContextCompat.getColor(mContext, colorResId);
            convertView.setBackgroundColor(color);
        }
        return convertView;
    }

    public void clear() {
        synchronized (mProgressLock) {
            stopFiltering();
            mAll = new ArrayList<>();
            if (mFilter == null) {
                mFiltered = null;
            } else {
                mFiltered = new ArrayList<>();
            }
        }
    }

    public void resetCommit() {
        clear();
        if (mFile == null) {
            GetCommitGraphTask getCommitGraphTask = new GetCommitGraphTask(mRepo, mAllBranches,
                    plotCommits -> {
                        if (plotCommits != null) {
                            synchronized (mProgressLock) {
                                stopFiltering();
                                ArrayList<RevCommit> all = new ArrayList<>(plotCommits.size());
                                for (PlotCommit<PlotLane> commit : plotCommits) {
                                    all.add(commit);
                                }
                                mAll = all;
                                doFiltering();
                            }
                        }
                    });
            getCommitGraphTask.executeTask();
            return;
        }
        GetCommitTask getCommitTask = new GetCommitTask(mRepo, mFile,
                new GetCommitCallback() {

                    @Override
                    public void postCommits(List<RevCommit> commits) {
                        if (commits != null) {
                            // TODO why == null
                            synchronized (mProgressLock) {
                                stopFiltering();
                                mAll = new ArrayList<>(commits);
                                doFiltering();
                            }
                        }
                    }
                });
        getCommitTask.executeTask();
    }

    /** Only meaningful for the main repo Commits tab -- see {@link #mAllBranches}. */
    public boolean supportsGraphMode() {
        return mFile == null;
    }

    public boolean isAllBranches() {
        return mAllBranches;
    }

    public void setAllBranches(boolean allBranches) {
        if (mAllBranches == allBranches) return;
        mAllBranches = allBranches;
        resetCommit();
    }

    private static class CommitsListItemHolder {
        public ImageView commitsIcon;
        public TextView commitsTitle;
        public TextView commitsMsg;
        public TextView commitAuthor;
        public TextView commitTime;
    }
}
