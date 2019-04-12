package com.pechuro.cashdebts.ui.fragment.localdebtlist

import android.os.Bundle
import android.view.View
import androidx.annotation.StringRes
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import com.pechuro.cashdebts.R
import com.pechuro.cashdebts.ui.activity.adddebt.AddDebtEvent
import com.pechuro.cashdebts.ui.activity.main.MainActivityEvent
import com.pechuro.cashdebts.ui.base.BaseFragment
import com.pechuro.cashdebts.ui.base.ItemTouchHelper
import com.pechuro.cashdebts.ui.fragment.localdebtlist.adapter.ItemSwipeCallback
import com.pechuro.cashdebts.ui.fragment.localdebtlist.adapter.LocalDebtListAdapter
import com.pechuro.cashdebts.ui.utils.EventBus
import io.reactivex.rxkotlin.addTo
import kotlinx.android.synthetic.main.fragment_local_debt_list.*
import javax.inject.Inject

class LocalDebtListFragment : BaseFragment<LocalDebtListFragmentViewModel>() {
    @Inject
    protected lateinit var adapter: LocalDebtListAdapter
    @Inject
    protected lateinit var layoutManager: RecyclerView.LayoutManager
    @Inject
    protected lateinit var swipeToDeleteHelper: ItemTouchHelper<ItemSwipeCallback.SwipeAction>

    override val layoutId: Int
        get() = R.layout.fragment_local_debt_list

    override fun getViewModelClass() = LocalDebtListFragmentViewModel::class

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupView()
        setViewListeners()
        setEventListeners()
    }

    override fun onStart() {
        super.onStart()
        setViewModelListeners()
    }

    private fun setupView() {
        recycler.apply {
            adapter = this@LocalDebtListFragment.adapter
            layoutManager = this@LocalDebtListFragment.layoutManager
        }
        swipeToDeleteHelper.attachToRecyclerView(recycler)
    }

    private fun setViewListeners() {
        fab_add.setOnClickListener {
            EventBus.publish(MainActivityEvent.OpenAddActivity(true))
        }
        recycler.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                if (dy > 0) fab_add.hide() else fab_add.show()
            }
        })
        swipeToDeleteHelper.actionEmitter.subscribe {
            when (it) {
                is ItemSwipeCallback.SwipeAction.SwipedToDelete -> deleteDebt(it.position)
                is ItemSwipeCallback.SwipeAction.SwipedToComplete -> {

                }
            }
        }.addTo(strongCompositeDisposable)
    }

    private fun setEventListeners() {
        EventBus.listen(AddDebtEvent::class.java).subscribe {
            when (it) {
                is AddDebtEvent.OnSuccess -> showSnackbar(R.string.msg_success)
            }
        }.addTo(strongCompositeDisposable)
    }

    private fun setViewModelListeners() {
        viewModel.debtSource.subscribe(adapter::update).addTo(weakCompositeDisposable)
    }

    private fun deleteDebt(position: Int) {
        val item = adapter.getItemByPosition(position)
        showUndoDeletionSnackbar()
        viewModel.deleteDebt(item)
    }

    private fun showSnackbar(@StringRes msgId: Int) {
        coordinator.postDelayed({
            Snackbar.make(coordinator, msgId, Snackbar.LENGTH_SHORT).show()
        }, SNACKBAR_SHOW_DELAY)
    }

    private fun showUndoDeletionSnackbar() {
        Snackbar.make(coordinator, R.string.msg_deleted, Snackbar.LENGTH_LONG)
            .setAction(R.string.action_undo) {
                viewModel.restoreDebt()
            }
            .show()
    }

    companion object {
        private const val SNACKBAR_SHOW_DELAY = 250L

        fun newInstance() = LocalDebtListFragment()
    }
}