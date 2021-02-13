package com.firebase.ui.firestore.paging;

import android.util.Log;

import com.firebase.ui.firestore.ClassSnapshotParser;
import com.firebase.ui.firestore.SnapshotParser;
import com.google.firebase.firestore.DocumentSnapshot;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.arch.core.util.Function;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.OnLifecycleEvent;
import androidx.lifecycle.Transformations;
import androidx.paging.PagedList;
import androidx.paging.PagedListAdapter;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

/**
 * Paginated RecyclerView Adapter for a Cloud Firestore query.
 *
 * Configured with {@link FirestorePagingOptions}.
 */
public abstract class FirestorePagingAdapter<T, VH extends RecyclerView.ViewHolder>
        extends PagedListAdapter<DocumentSnapshot, VH>
        implements LifecycleObserver {

    private static final String TAG = "FirestorePagingAdapter";

    //Error observer to determine last occurred Error
    private final Observer<Exception> mErrorObserver = new Observer<Exception>() {
        @Override
        public void onChanged(@Nullable Exception e) {
            onError(e);
        }
    };
    private final Observer<LoadingState> mStateObserver =
            new Observer<LoadingState>() {
                @Override
                public void onChanged(@Nullable LoadingState state) {
                    if (state == null) {
                        return;
                    }

                    onLoadingStateChanged(state);
                }
            };
    private final Observer<PagedList<DocumentSnapshot>> mDataObserver =
            new Observer<PagedList<DocumentSnapshot>>() {
                @Override
                public void onChanged(@Nullable PagedList<DocumentSnapshot> snapshots) {
                    if (snapshots == null) {
                        return;
                    }

                    submitList(snapshots);
                }
            };

    private SnapshotParser<T> mParser;
    private LifecycleOwner mOwner;
    private LiveData<PagedList<DocumentSnapshot>> mSnapshots;
    private LiveData<LoadingState> mLoadingState;
    private LiveData<Exception> mException;
    private MutableLiveData<FirestoreDataSource> mDataSource;

    /**
     * Construct a new FirestorePagingAdapter from the given {@link FirestorePagingOptions}.
     */
    public FirestorePagingAdapter(@NonNull FirestorePagingOptions<T> options) {
        super(options.getDiffCallback());

        initWithFirebaseOptions(options);
    }

    public FirestorePagingAdapter(@Nullable LifecycleOwner owner, @NonNull Class<T> modelClass) {
        this(owner, new ClassSnapshotParser<>(modelClass));
    }

    public FirestorePagingAdapter(@Nullable LifecycleOwner owner,
                                  @NonNull Class<T> modelClass,
                                  @NonNull DiffUtil.ItemCallback<DocumentSnapshot> diffCallback) {
        this(owner, new ClassSnapshotParser<>(modelClass), diffCallback);
    }

    public FirestorePagingAdapter(@Nullable LifecycleOwner owner, @NonNull SnapshotParser<T> parser) {
        super(new DefaultSnapshotDiffCallback<>(parser));
        init(owner, parser);
    }

    public FirestorePagingAdapter(@Nullable LifecycleOwner owner,
                                  @NonNull SnapshotParser<T> parser,
                                  @NonNull DiffUtil.ItemCallback<DocumentSnapshot> diffCallback) {
        super(diffCallback);
        init(owner, parser);
    }

    /**
     * Initializes Snapshots and LiveData
     */
    private void init(@Nullable LifecycleOwner owner, @NonNull SnapshotParser<T> parser) {
        mParser = parser;
        mOwner = owner;
        if (mOwner != null) {
            mOwner.getLifecycle().addObserver(this);
        }
        mDataSource = new MutableLiveData<>();
    }

    private void initWithFirebaseOptions(@NonNull FirestorePagingOptions<T> options) {
        mSnapshots = options.getData();

        mLoadingState = Transformations.switchMap(mSnapshots,
                new Function<PagedList<DocumentSnapshot>, LiveData<LoadingState>>() {
                    @Override
                    public LiveData<LoadingState> apply(PagedList<DocumentSnapshot> input) {
                        FirestoreDataSource dataSource = (FirestoreDataSource) input.getDataSource();
                        return dataSource.getLoadingState();
                    }
                });

        mException = Transformations.switchMap(mSnapshots,
                new Function<PagedList<DocumentSnapshot>, LiveData<Exception>>() {
                    @Override
                    public LiveData<Exception> apply(PagedList<DocumentSnapshot> input) {
                        FirestoreDataSource dataSource = (FirestoreDataSource) input.getDataSource();
                        return dataSource.getLastError();
                    }
                });
        init(options.getOwner(), options.getParser());
    }

    @Override
    public void submitList(PagedList<DocumentSnapshot> pagedList) {
        FirestoreDataSource source = (FirestoreDataSource) pagedList.getDataSource();
        mLoadingState = source.getLoadingState();
        mException = source.getLastError();
        mDataSource.postValue(source);
        mLoadingState.observeForever(mStateObserver);
        mException.observeForever(mErrorObserver);
        super.submitList(pagedList);
    }

    /**
     * If {@link #onLoadingStateChanged(LoadingState)} indicates error state, call this method to
     * attempt to retry the most recent failure.
     */
    public void retry() {
        FirestoreDataSource source = mDataSource.getValue();
        if (source == null) {
            Log.w(TAG, "Called retry() when FirestoreDataSource is null!");
            return;
        }

        source.retry();
    }

    /**
     * To attempt to refresh the list. It will reload the list from beginning.
     */
    public void refresh() {
        FirestoreDataSource mFirebaseDataSource = mDataSource.getValue();
        if (mFirebaseDataSource == null) {
            Log.w(TAG, "Called refresh() when FirestoreDataSource is null!");
            return;
        }
        mFirebaseDataSource.invalidate();
    }

    /**
     * Re-initialize the Adapter with a new set of options. Can be used to change the query without
     * re-constructing the entire adapter.
     */
    public void updateOptions(@NonNull FirestorePagingOptions<T> options) {

        // Tear down old options
        boolean hasObservers;
        if (mSnapshots != null) {
            hasObservers = mSnapshots.hasObservers();
        } else {
            hasObservers = false;
        }
        if (mOwner != null) {
            mOwner.getLifecycle().removeObserver(this);
        }
        stopListening();

        // Reinit Options
        initWithFirebaseOptions(options);

        if (hasObservers) {
            startListening();
        }
    }

    /**
     * Start listening to paging / scrolling events and populating adapter data.
     */
    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    public void startListening() {
        if (mSnapshots != null) {
            mSnapshots.observeForever(mDataObserver);
            mLoadingState.observeForever(mStateObserver);
            mException.observeForever(mErrorObserver);
        }
    }

    /**
     * Unsubscribe from paging / scrolling events, no more data will be populated, but the existing
     * data will remain.
     */
    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    public void stopListening() {
        if (mSnapshots != null) {
            mSnapshots.removeObserver(mDataObserver);
        }
        if (mLoadingState != null) {
            mLoadingState.removeObserver(mStateObserver);
        }
        if (mException != null) {
            mException.removeObserver(mErrorObserver);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        DocumentSnapshot snapshot = getItem(position);
        onBindViewHolder(holder, position, mParser.parseSnapshot(snapshot));
    }

    /**
     * @param model the model object containing the data that should be used to populate the view.
     * @see #onBindViewHolder(RecyclerView.ViewHolder, int)
     */
    protected abstract void onBindViewHolder(@NonNull VH holder, int position, @NonNull T model);

    /**
     * Called whenever the loading state of the adapter changes.
     * <p>
     * When the state is {@link LoadingState#ERROR} the adapter will stop loading any data unless
     * {@link #retry()} is called.
     */
    protected void onLoadingStateChanged(@NonNull LoadingState state) {
        // For overriding
    }

    /**
     * Called whenever the {@link Exception} is caught.
     * <p>
     * When {@link Exception} is caught the adapter will stop loading any data
     */
    protected void onError(@NonNull Exception e) {
        Log.w(TAG, "onError", e);
    }
}
