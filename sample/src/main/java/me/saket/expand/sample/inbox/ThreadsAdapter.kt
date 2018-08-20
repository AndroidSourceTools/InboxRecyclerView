package me.saket.expand.sample.inbox

import android.annotation.SuppressLint
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewStub
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.jakewharton.rxrelay2.PublishRelay
import me.saket.expand.sample.Attachment
import me.saket.expand.sample.EmailThread
import me.saket.expand.sample.R
import me.saket.expand.sample.inbox.ThreadsAdapter.ViewType.NORMAL
import me.saket.expand.sample.inbox.ThreadsAdapter.ViewType.WITH_IMAGE_ATTACHMENTS
import me.saket.expand.sample.inbox.ThreadsAdapter.ViewType.values

class ThreadsAdapter : ListAdapter<EmailThread, EmailViewHolder>(EmailThread.ItemDiffer()) {

  enum class ViewType {
    NORMAL,
    WITH_IMAGE_ATTACHMENTS
  }

  val itemClicks = PublishRelay.create<EmailThreadClicked>()!!

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EmailViewHolder {
    val threadLayout = LayoutInflater.from(parent.context).inflate(R.layout.list_email_thread, parent, false)
    return when (values()[viewType]) {
      NORMAL -> EmailViewHolder(threadLayout, itemClicks)
      WITH_IMAGE_ATTACHMENTS -> EmailViewHolderWithImageAttachments(threadLayout, itemClicks)
    }
  }

  override fun onBindViewHolder(holder: EmailViewHolder, position: Int) {
    holder.emailThread = getItem(position)
    holder.render()
  }

  override fun getItemId(position: Int): Long {
    return position.toLong()
  }

  override fun getItemViewType(position: Int): Int {
    val latestEmail = getItem(position).emails.last()
    return when {
      latestEmail.hasImageAttachments -> WITH_IMAGE_ATTACHMENTS.ordinal
      else -> NORMAL.ordinal
    }
  }
}

open class EmailViewHolder(
    itemView: View,
    itemClicks: PublishRelay<EmailThreadClicked>
) : RecyclerView.ViewHolder(itemView) {

  private val bylineTextView = itemView.findViewById<TextView>(R.id.emailthread_item_byline)
  private val subjectTextView = itemView.findViewById<TextView>(R.id.emailthread_item_subject)
  private val bodyTextView = itemView.findViewById<TextView>(R.id.emailthread_item_body)
  private val avatarImageView = itemView.findViewById<ImageView>(R.id.emailthread_item_avatar)

  lateinit var emailThread: EmailThread

  init {
    itemView.setOnClickListener {
      itemClicks.accept(EmailThreadClicked(emailThread, adapterPosition, itemId))
    }
  }

  @SuppressLint("SetTextI18n")
  open fun render() {
    val latestEmail = emailThread.emails.last()
    bylineTextView.text = "${emailThread.sender.name} — ${latestEmail.timestamp}"

    subjectTextView.text = emailThread.subject
    val subjectTextSize = subjectTextView.resources.getDimensionPixelSize(when {
      latestEmail.hasImageAttachments -> R.dimen.emailthread_subject_textize_with_photo_attachments
      else -> R.dimen.emailthread_subject_textize_without_photo_attachments
    })
    subjectTextView.setTextSize(TypedValue.COMPLEX_UNIT_PX, subjectTextSize.toFloat())

    bodyTextView.apply {
      text = latestEmail.body.replace("\n", " ")
      visibility = if (latestEmail.showBodyInThreads) View.VISIBLE else View.GONE

      when {
        latestEmail.hasNonImageAttachments -> setDrawableStart(R.drawable.ic_attachment_24dp)
        else -> setDrawableStart(null)
      }
    }

    avatarImageView.setImageResource(emailThread.sender.profileImageRes!!)
  }
}

class EmailViewHolderWithImageAttachments(
    itemView: View,
    itemClicks: PublishRelay<EmailThreadClicked>
) : EmailViewHolder(itemView, itemClicks) {

  private val imageAttachmentsStub = itemView.findViewById<ViewStub>(R.id.emailthread_image_attachments_stub)
  private val imageAttachmentsRecyclerView by lazy { itemView.findViewById<RecyclerView>(R.id.emailthread_image_attachments) }
  private val adapter = ImageAttachmentAdapter(clickListener = {
    itemClicks.accept(EmailThreadClicked(emailThread, adapterPosition, itemId))
  })

  init {
    imageAttachmentsStub.inflate()
    imageAttachmentsRecyclerView.layoutManager = LinearLayoutManager(itemView.context, LinearLayoutManager.HORIZONTAL, false)
    imageAttachmentsRecyclerView.adapter = adapter
  }

  override fun render() {
    super.render()
    val latestEmail = emailThread.emails.last()
    adapter.images = latestEmail.attachments.filterIsInstance(Attachment.Image::class.java)
  }
}