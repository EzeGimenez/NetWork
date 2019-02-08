package com.visoft.network.tab_search;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.widget.SearchView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.gms.maps.model.LatLng;
import com.visoft.network.R;
import com.visoft.network.funcionalidades.SearcherProUser;

public class HolderFirstTab extends Fragment {

    private static SearcherProUser searcherProUser;
    private final int containerId = R.id.ContainerFirstTab;
    private FragmentManager fragmentManager;
    private FragmentFirstTab fragmentGeneral, fragmentEspecifico, fragmentSearchResult;
    private String actual;
    private String idGeneral = "general";
    private String idSearchResult = "searchResult";
    private SearchView.OnQueryTextListener listenerSearchView;

    private TabLayout tabLayout;
    private int current;

    private String currentQuery;

    public SearcherProUser getSearcherProUser() {
        return searcherProUser;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        searcherProUser = new SearcherProUser();
        fragmentManager = getChildFragmentManager();
        iniciarFragments();

        listenerSearchView = new SearchView.OnQueryTextListener() {

            private String prev = "";

            @Override
            public boolean onQueryTextSubmit(String s) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String s) {
                if (s.length() > 0) {
                    currentQuery = s;
                    if (actual != null && !actual.equals(idSearchResult)) {
                        prev = actual;
                        Bundle bundle = new Bundle();
                        bundle.putString("name", s);
                        fragmentSearchResult.setArguments(bundle);
                        fragmentManager
                                .beginTransaction()
                                .addToBackStack(actual)
                                .setCustomAnimations(R.anim.enter_from_right, R.anim.exit_to_left, R.anim.enter_from_left, R.anim.exit_to_right)
                                .replace(containerId, fragmentSearchResult, idSearchResult)
                                .commit();

                    } else {
                        ((FragmentSearchResults) fragmentSearchResult).filter(s);
                    }
                } else if (!prev.equals("")) {

                    fragmentManager.popBackStack();
                    prev = "";

                } else {
                    ((FragmentSearchResults) fragmentSearchResult).filter(s);
                }

                return true;
            }
        };

        fragmentManager.beginTransaction()
                .add(containerId, fragmentGeneral, idGeneral).commit();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_holder_first_tab, container, false);
    }

    public void setLocation(LatLng l) {
        if (searcherProUser != null) {
            searcherProUser.setLocation(l);
        }
    }

    public void iniciarFragments() {
        if (fragmentSearchResult == null) {
            fragmentSearchResult = new FragmentSearchResults();
            fragmentGeneral = new FragmentGeneral();
            fragmentEspecifico = new FragmentSpecific();
        }

        fragmentSearchResult.setHolder(this);
        fragmentGeneral.setHolder(this);
        fragmentEspecifico.setHolder(this);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        tabLayout = view.findViewById(R.id.tabLayout);
        current = 0;
        tabLayout.addTab(tabLayout.newTab());
        tabLayout.addTab(tabLayout.newTab());
        tabLayout.addTab(tabLayout.newTab());

        //SEARCH VIEW
        SearchView searchView = view.findViewById(R.id.searchView);
        searchView.setOnQueryTextListener(listenerSearchView);
    }

    public void onBackPressed() {
        if (fragmentManager.getBackStackEntryCount() > 0) {
            fragmentManager.popBackStack();
            tabLayout.getTabAt(--current).select();
        } else {
            getActivity().finishAffinity();
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        if (currentQuery != null) {
            if (actual.equals(idSearchResult)) {
                ((FragmentSearchResults) fragmentSearchResult).filter(currentQuery);
            }
        }
    }

    public void advance(Bundle bundle) {
        String idEspecifico = "esp";

        current++;
        tabLayout.getTabAt(current).select();

        if (actual.equals(idGeneral)) {

            fragmentEspecifico.setArguments(bundle);

            fragmentManager
                    .beginTransaction()
                    .addToBackStack(idGeneral)
                    .setCustomAnimations(R.anim.enter_from_right, R.anim.exit_to_left, R.anim.enter_from_left, R.anim.exit_to_right)
                    .replace(containerId, fragmentEspecifico, idEspecifico)
                    .commit();

        } else if (actual.equals(idEspecifico)) {

            fragmentSearchResult.setArguments(bundle);

            fragmentManager
                    .beginTransaction()
                    .addToBackStack(idEspecifico)
                    .setCustomAnimations(R.anim.enter_from_right, R.anim.exit_to_left, R.anim.enter_from_left, R.anim.exit_to_right)
                    .replace(containerId, fragmentSearchResult, idSearchResult)
                    .commit();
        }
    }

    public void setCurrentQuery(String a) {
        this.currentQuery = a;
    }

    public void setActual(String actual) {
        this.actual = actual;
    }
}