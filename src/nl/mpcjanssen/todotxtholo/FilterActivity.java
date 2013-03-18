package nl.mpcjanssen.todotxtholo;

import android.app.*;
import android.app.ActionBar.Tab;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import nl.mpcjanssen.todotxtholo.util.Util;

import java.util.ArrayList;

public class FilterActivity extends Activity {

	final static String TAG = FilterActivity.class.getSimpleName();
	Menu menu;
	private ActionBar actionbar;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		Log.v(TAG, "Called with intent: " + getIntent().toString());
		super.onCreate(savedInstanceState);
		setContentView(R.layout.filter);
		Bundle arguments;
		actionbar = getActionBar();
		actionbar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);

		// Fill arguments for fragment
		arguments = new Bundle();

		arguments.putStringArrayList(Constants.ITEMS, getIntent()
				.getStringArrayListExtra(Constants.EXTRA_CONTEXTS));

		arguments.putStringArrayList(
				Constants.INITIAL_SELECTED_ITEMS,
				getIntent().getStringArrayListExtra(
						Constants.EXTRA_CONTEXTS_SELECTED));
		arguments.putBoolean(Constants.INITIAL_NOT, getIntent()
				.getBooleanExtra(Constants.EXTRA_CONTEXTS + "not", false));
		actionbar.addTab(actionbar
				.newTab()
				.setText(getString(R.string.context_prompt))
				.setTabListener(
						new MyTabsListener(this, Constants.EXTRA_CONTEXTS,
								FilterListFragment.class, arguments))
				.setTag(Constants.EXTRA_CONTEXTS));

		// Fill arguments for fragment
		arguments = new Bundle();

		arguments.putStringArrayList(Constants.ITEMS, getIntent()
				.getStringArrayListExtra(Constants.EXTRA_PROJECTS));

		arguments.putStringArrayList(
				Constants.INITIAL_SELECTED_ITEMS,
				getIntent().getStringArrayListExtra(
						Constants.EXTRA_PROJECTS_SELECTED));
		arguments.putBoolean(Constants.INITIAL_NOT, getIntent()
				.getBooleanExtra(Constants.EXTRA_PROJECTS + "not", false));
		actionbar.addTab(actionbar
				.newTab()
				.setText(getString(R.string.project_prompt))
				.setTabListener(
						new MyTabsListener(this, Constants.EXTRA_PROJECTS,
								FilterListFragment.class, arguments))
				.setTag(Constants.EXTRA_PROJECTS));

		// Fill arguments for fragment
		arguments = new Bundle();

		arguments.putStringArrayList(Constants.ITEMS, getIntent()
				.getStringArrayListExtra(Constants.EXTRA_PRIORITIES));

		arguments.putStringArrayList(
				Constants.INITIAL_SELECTED_ITEMS,
				getIntent().getStringArrayListExtra(
						Constants.EXTRA_PRIORITIES_SELECTED));
		arguments.putBoolean(Constants.INITIAL_NOT, getIntent()
				.getBooleanExtra(Constants.EXTRA_PRIORITIES + "not", false));
		actionbar.addTab(actionbar
				.newTab()
				.setText(getString(R.string.priority_short_prompt))
				.setTabListener(
						new MyTabsListener(this, Constants.EXTRA_PRIORITIES,
								FilterListFragment.class, arguments))
				.setTag(Constants.EXTRA_PRIORITIES));

		// Fill arguments for fragment
		arguments = new Bundle();
		arguments.putInt(Constants.ITEMS, R.array.sort);
		arguments.putStringArrayList(
				Constants.INITIAL_SELECTED_ITEMS,
				getIntent().getStringArrayListExtra(Constants.EXTRA_SORT_SELECTED));
		actionbar.addTab(actionbar
				.newTab()
				.setText(getString(R.string.sort))
				.setTabListener(
						new MyTabsListener(this, getString(R.string.sort),
								FilterItemFragment.class, arguments))
				.setTag(getString(R.string.sort)));
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.filter, menu);
		this.menu = menu;
		return true;
	}

	@Override
	public boolean onMenuItemSelected(int featureId, MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_apply_filter:

			applyFilter();

			break;
		case R.id.menu_select_all:
			selectAll();
			break;
		case R.id.menu_clear_all:
			clearAll();
			break;
		case R.id.menu_add_filter_shortcut:
			createFilterShortcut();
			break;
		}
		return true;
	}

	private void selectAll() {
		String tag = (String) actionbar.getSelectedTab().getTag();
		if (!tag.equals(getString(R.string.sort))) {
			FilterListFragment fr = (FilterListFragment) getFragmentManager()
					.findFragmentByTag(tag);
			fr.selectAll();
		}
	}

	private void clearAll() {
		String tag = (String) actionbar.getSelectedTab().getTag();
		if (!tag.equals(getString(R.string.sort))) {
			FilterListFragment fr = (FilterListFragment) getFragmentManager()
					.findFragmentByTag(tag);
			fr.clearAll();
		}
	}

	// Safe the active tab on configuration changes
	@Override
	public void onSaveInstanceState(Bundle savedInstanceState) {
		super.onSaveInstanceState(savedInstanceState);
		savedInstanceState.putInt("active_tab",
				actionbar.getSelectedNavigationIndex());
	}

	@Override
	public void onRestoreInstanceState(Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);
		actionbar.setSelectedNavigationItem(savedInstanceState
                .getInt("active_tab"));
	}

	private Intent createFilterIntent() {
		Intent target = new Intent(Constants.INTENT_START_FROM_SHORTCUT);
		String name = "";
		ArrayList<String> appliedFilters = new ArrayList<String>();
		ArrayList<String> contextFilter = getFilter(Constants.EXTRA_CONTEXTS);
		ArrayList<String> projectsFilter = getFilter(Constants.EXTRA_PROJECTS);
		ArrayList<String> prioritiesFilter = getFilter(Constants.EXTRA_PRIORITIES);
        ArrayList<String> selectedSortFilters = getSortFilter(getString(R.string.sort));
		appliedFilters.addAll(contextFilter);
		appliedFilters.addAll(prioritiesFilter);
		appliedFilters.addAll(projectsFilter);

		target.putExtra(Constants.INTENT_CONTEXTS_FILTER,
				Util.join(contextFilter, "\n"));
		target.putExtra(Constants.INTENT_CONTEXTS_FILTER_NOT,
				getNot(Constants.EXTRA_CONTEXTS));
		target.putExtra(Constants.INTENT_PROJECTS_FILTER,
				Util.join(projectsFilter, "\n"));
		target.putExtra(Constants.INTENT_PROJECTS_FILTER_NOT,
				getNot(Constants.EXTRA_PROJECTS));
		target.putExtra(Constants.INTENT_PRIORITIES_FILTER,
				Util.join(prioritiesFilter, "\n"));
		target.putExtra(Constants.INTENT_PRIORITIES_FILTER_NOT,
				getNot(Constants.EXTRA_PRIORITIES));
		target.putExtra(Constants.INTENT_ACTIVE_SORT,
                Util.join(selectedSortFilters, "\n"));

		if (appliedFilters.size() == 1) {
			name = appliedFilters.get(0);
		}
		target.putExtra("name", name);
		return target;
	}

    private ArrayList<String> getSortFilter(String tag) {
        FilterItemFragment fr;
        fr = (FilterItemFragment) this.getFragmentManager().findFragmentByTag(
                tag);
        ArrayList<String> filter;
        if (fr == null) {
            // fragment was never intialized
            filter = getIntent().getStringArrayListExtra(tag + "_SELECTED");
        } else {
            filter = fr.getSelectedItems();
        }
        if (filter == null) {
            filter = new ArrayList<String>();
        }
        return filter;
    }

    private ArrayList<String> getFilter(String tag) {
		FilterListFragment fr;
		fr = (FilterListFragment) this.getFragmentManager().findFragmentByTag(
				tag);
		ArrayList<String> filter;
		if (fr == null) {
			// fragment was never intialized
			filter = getIntent().getStringArrayListExtra(tag + "_SELECTED");
		} else {
			filter = fr.getSelectedItems();
		}
		if (filter == null) {
			filter = new ArrayList<String>();
		}
		return filter;
	}

	private boolean getNot(String tag) {
		FilterListFragment fr;
		fr = (FilterListFragment) this.getFragmentManager().findFragmentByTag(
				tag);
		boolean not;
		if (fr == null) {
			// fragment was never intialized
			not = getIntent().getBooleanExtra(tag + "not", false);
		} else {
			not = fr.getNot();
		}
		return not;
	}

	private void applyFilter() {
		Intent data = createFilterIntent();
		startActivity(data);
	}

	private void createFilterShortcut() {
		final Intent shortcut = new Intent(
				"com.android.launcher.action.INSTALL_SHORTCUT");
		Intent target = createFilterIntent();

		// Setup target intent for shortcut
		shortcut.putExtra(Intent.EXTRA_SHORTCUT_INTENT, target);

		// Set shortcut icon
		Intent.ShortcutIconResource iconRes = Intent.ShortcutIconResource
				.fromContext(this, R.drawable.icon);
		shortcut.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE, iconRes);

		AlertDialog.Builder alert = new AlertDialog.Builder(this);

		alert.setTitle("Create shortcut");
		alert.setMessage("Shortcut name");

		// Set an EditText view to get user input
		final EditText input = new EditText(this);
		alert.setView(input);
		input.setText(target.getStringExtra("name"));

		alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
				String value = input.getText().toString();
				if (value.equals("")) {
					Util.showToastShort(getApplicationContext(),
							R.string.shortcut_name_empty);
				} else {
					shortcut.putExtra(Intent.EXTRA_SHORTCUT_NAME, value);
					sendBroadcast(shortcut);
				}
			}
		});

		alert.setNegativeButton("Cancel",
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						// Canceled.
					}
				});

		alert.show();

	}

	private class MyTabsListener<T extends Fragment> implements
			ActionBar.TabListener {

		private Fragment mFragment;
		private final Activity mActivity;
		private final String mTag;
		private final Bundle mArguments;
		private Class<T> mClz;

		public MyTabsListener(Activity activity, String tag, Class<T> clz,
				Bundle arguments) {
			mActivity = activity;
			mTag = tag;
			mArguments = arguments;
			mClz = clz;
			// Check to see if we already have a fragment for this tab, probably
			// from a previously saved state.
			mFragment = mActivity.getFragmentManager().findFragmentByTag(mTag);
		}

		@Override
		public void onTabReselected(Tab tab, FragmentTransaction ft) {

		}

		@Override
		public void onTabSelected(Tab tab, FragmentTransaction ft) {
			// Check if the fragment is already initialized
			if (mFragment == null) {
				// If not, instantiate and add it to the activity
				mFragment = Fragment.instantiate(mActivity, mClz.getName(),
						mArguments);
				ft.add(android.R.id.content, mFragment, mTag);
			} else {
				// If it exists, simply attach it in order to show it
				ft.attach(mFragment);
			}
		}

		@Override
		public void onTabUnselected(Tab tab, FragmentTransaction ft) {
			if (mFragment != null) {
				// Detach the fragment, because another one is being attached
				ft.detach(mFragment);
			}
		}

	}
}
