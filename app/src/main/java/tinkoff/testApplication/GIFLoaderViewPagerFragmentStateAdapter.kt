package tinkoff.testApplication

import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import tinkoff.testApplication.models.GIFCategory

class GIFLoaderViewPagerFragmentStateAdapter(fragmentActivity: FragmentActivity) :
    FragmentStateAdapter(fragmentActivity) {

    private val categories = listOf(
        GIFCategory("Последние","latest"),
        GIFCategory("Лучшие","top"),
        GIFCategory("Случайные","random")
    )


    override fun createFragment(position: Int): Fragment {
        return GIFLoaderFragment().apply {
            arguments = bundleOf(
                "name" to categories[position].name,
                "link" to categories[position].link
            )
        }
    }

    override fun getItemCount(): Int = categories.size
}