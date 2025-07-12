// Dán toàn bộ code này để thay thế file ReportAdapter.java cũ

package com.example.baicuoiky04;

import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class ReportAdapter extends RecyclerView.Adapter<ReportAdapter.ViewHolder> {

    // Interface đã được cập nhật với đúng chữ ký phương thức
    public interface OnReportActionListener {
        void onViewContent(DataModels.Report report);
        void onSuspendUser(String userId);
        void onUnsuspendUser(String userId);
        void onRemoveReview(DataModels.Report report, String reportId); // Chữ ký đã đúng
        void onDismissReport(String reportId);
    }

    private Context context;
    private List<DataModels.Report> reportList;
    private List<String> reportIdList;
    private OnReportActionListener listener;

    public ReportAdapter(Context context, List<DataModels.Report> reportList, List<String> reportIdList, OnReportActionListener listener) {
        this.context = context;
        this.reportList = reportList;
        this.reportIdList = reportIdList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.list_item_report, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        DataModels.Report report = reportList.get(position);
        String reportId = reportIdList.get(position);
        holder.bind(report, reportId);
    }

    @Override
    public int getItemCount() {
        return reportList.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        TextView textViewReason, textViewReportedContent, textViewReporter, textViewComment;
        Button btnViewContent, btnSuspendUser, btnDismiss;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewReason = itemView.findViewById(R.id.textViewReason);
            textViewReportedContent = itemView.findViewById(R.id.textViewReportedContent);
            textViewReporter = itemView.findViewById(R.id.textViewReporter);
            textViewComment = itemView.findViewById(R.id.textViewComment);
            btnViewContent = itemView.findViewById(R.id.btnViewContent);
            btnSuspendUser = itemView.findViewById(R.id.btnSuspendUser);
            btnDismiss = itemView.findViewById(R.id.btnDismiss);
        }

        void bind(DataModels.Report report, String reportId) {
            textViewReason.setText("Lý do: " + report.getReason());
            textViewReporter.setText("Người báo cáo: " + report.getReporterId());

            if (!TextUtils.isEmpty(report.getComment())) {
                textViewComment.setVisibility(View.VISIBLE);
                textViewComment.setText("Bình luận: " + report.getComment());
            } else {
                textViewComment.setVisibility(View.GONE);
            }

            // ================== LOGIC ĐIỀU KHIỂN NÚT VÀ TEXT ĐÃ ĐƯỢC CẬP NHẬT ==================
            DataModels.User reportedUser = report.getReportedUserObject();

            // Trường hợp 1: Báo cáo một bài đánh giá (Review)
            if (!TextUtils.isEmpty(report.getReportedReviewId())) {
                textViewReportedContent.setText("Báo cáo đánh giá của user: " + report.getReportedUserId());
                btnSuspendUser.setVisibility(View.VISIBLE);
                btnSuspendUser.setText("Xóa Review");
                btnSuspendUser.setTextColor(ContextCompat.getColor(context, R.color.status_sold)); // Màu đỏ
                // Gọi đúng hàm listener với đúng tham số
                btnSuspendUser.setOnClickListener(v -> listener.onRemoveReview(report, reportId));
            }
            // Trường hợp 2: Báo cáo người dùng hoặc tin đăng (có thông tin user)
            else if (reportedUser != null) {
                btnSuspendUser.setVisibility(View.VISIBLE);

                // Cập nhật text nội dung báo cáo
                if (!TextUtils.isEmpty(report.getReportedListingId())) {
                    textViewReportedContent.setText("Báo cáo tin đăng: " + report.getReportedListingId());
                } else {
                    textViewReportedContent.setText("Báo cáo người dùng: " + report.getReportedUserId());
                }

                // Logic khóa/mở khóa user
                if ("suspended".equals(reportedUser.getAccountStatus())) {
                    btnSuspendUser.setText("Mở khóa");
                    btnSuspendUser.setTextColor(ContextCompat.getColor(context, R.color.status_available)); // Màu xanh
                    btnSuspendUser.setOnClickListener(v -> listener.onUnsuspendUser(report.getReportedUserId()));
                } else {
                    btnSuspendUser.setText("Khóa User");
                    btnSuspendUser.setTextColor(ContextCompat.getColor(context, R.color.status_sold)); // Màu đỏ
                    btnSuspendUser.setOnClickListener(v -> listener.onSuspendUser(report.getReportedUserId()));
                }
            }
            // Trường hợp 3: Không có thông tin để thực hiện hành động
            else {
                textViewReportedContent.setText("Báo cáo không xác định");
                btnSuspendUser.setVisibility(View.GONE);
            }
            // ====================================================================================

            btnViewContent.setOnClickListener(v -> listener.onViewContent(report));
            btnDismiss.setOnClickListener(v -> listener.onDismissReport(reportId));
        }
    }
}