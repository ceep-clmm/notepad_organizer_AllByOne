package com.example.allbyone.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.allbyone.R;
import com.example.allbyone.models.Project;

import java.util.List;

public class ProjectAdapter extends RecyclerView.Adapter<ProjectAdapter.ProjectViewHolder> {

    private final List<Project> projectList;
    private final Context context;
    private final OnProjectClickListener onProjectClickListener;

    private final OnProjectLongClickListener onProjectLongClickListener;

    public ProjectAdapter(Context context, List<Project> projectList,
                          OnProjectClickListener onProjectClickListener,
                          OnProjectLongClickListener onProjectLongClickListener) {
        this.context = context;
        this.projectList = projectList;
        this.onProjectClickListener = onProjectClickListener;
        this.onProjectLongClickListener = onProjectLongClickListener;
    }


    @NonNull
    @Override
    public ProjectViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_project, parent, false);
        return new ProjectViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ProjectViewHolder holder, int position) {
        Project project = projectList.get(position);
        holder.projectName.setText(project.name);

        switch (project.type) {
            case "note":
                holder.projectIcon.setImageResource(R.drawable.ic_note);
                break;
            case "list":
                holder.projectIcon.setImageResource(R.drawable.ic_list);
                break;
            case "canvas":
                holder.projectIcon.setImageResource(R.drawable.ic_canvas);
                break;
        }

        holder.itemView.setOnClickListener(v -> onProjectClickListener.onProjectClick(project));
        holder.itemView.setOnLongClickListener(v -> {
            onProjectLongClickListener.onProjectLongClick(project);
            return true;
        });
    }


    @Override
    public int getItemCount() {
        return projectList.size();
    }

    public interface OnProjectClickListener {
        void onProjectClick(Project project);
    }

    public interface OnProjectLongClickListener {
        void onProjectLongClick(Project project);
    }


    static class ProjectViewHolder extends RecyclerView.ViewHolder {
        TextView projectName;
        ImageView projectIcon;

        public ProjectViewHolder(@NonNull View itemView) {
            super(itemView);
            projectName = itemView.findViewById(R.id.projectName);
            projectIcon = itemView.findViewById(R.id.projectIcon);
        }
    }
}
