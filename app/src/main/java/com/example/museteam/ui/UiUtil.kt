package com.example.museteam.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import com.example.museteam.R
import com.example.museteam.data.ActivityState

object UiUtil {

    /** Màu chấm trạng thái theo mockup: chạy xanh · chờ vàng · kẹt đỏ · nghỉ xám · xong xanh dương. */
    fun stateColorRes(state: ActivityState): Int = when (state) {
        ActivityState.WORKING -> R.color.ok_green
        ActivityState.WAITING -> R.color.warn_amber
        ActivityState.STUCK -> R.color.error_red
        ActivityState.IDLE -> R.color.idle_gray
        ActivityState.DONE -> R.color.done_sky
    }

    /**
     * Mở nhanh Muse: mở thẳng muse.ai (trình duyệt/app Muse nếu máy có).
     * App ngoài KHÔNG ép Muse đổi sang đúng tài khoản được — chỉ rút ngắn đường đi.
     */
    fun openMuse(context: Context) {
        try {
            context.startActivity(
                Intent(Intent.ACTION_VIEW, Uri.parse("https://muse.ai"))
            )
        } catch (e: Exception) {
            Toast.makeText(context, "Không mở được muse.ai trên máy này", Toast.LENGTH_SHORT)
                .show()
        }
    }
}
