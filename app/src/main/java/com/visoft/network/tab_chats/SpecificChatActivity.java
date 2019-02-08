package com.visoft.network.tab_chats;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.gson.Gson;
import com.visoft.network.R;
import com.visoft.network.custom_views.CustomDialog;
import com.visoft.network.funcionalidades.GsonerMessages;
import com.visoft.network.funcionalidades.HolderCurrentAccountManager;
import com.visoft.network.funcionalidades.LoadingScreen;
import com.visoft.network.funcionalidades.Messenger;
import com.visoft.network.objects.ChatOverview;
import com.visoft.network.objects.Message;
import com.visoft.network.objects.MessageContractFinished;
import com.visoft.network.objects.User;
import com.visoft.network.objects.ViewHolderChats;
import com.visoft.network.profiles.ProfileActivity;
import com.visoft.network.profiles.UserReviewActivity;
import com.visoft.network.util.Constants;
import com.visoft.network.util.Database;
import com.visoft.network.util.GlideApp;

import de.hdodenhof.circleimageview.CircleImageView;


public class SpecificChatActivity extends AppCompatActivity {
    public static boolean isRunning;
    private User receiver;
    private FirebaseRecyclerAdapter<String, ViewHolderChats> recyclerViewAdapter;
    private Messenger m;

    //Componentes gráficas
    private RecyclerView recyclerView;

    private static float dpToPx(Context context, float valueInDp) {
        DisplayMetrics metrics = context.getResources().getDisplayMetrics();
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, valueInDp, metrics);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.specific_chat_activity);

        LoadingScreen loadingScreen = new LoadingScreen(this, (ViewGroup) findViewById(R.id.rootView));
        receiver = (User) getIntent().getSerializableExtra("receiver");

        final DatabaseReference database = Database.getDatabase().getReference();
        recyclerView = findViewById(R.id.listViewSpecificChat);
        loadingScreen.show();

        final ChatOverview c = (ChatOverview) getIntent().getSerializableExtra("chatOverview");

        if (!c.isFinished()) {
            m = new Messenger(this, HolderCurrentAccountManager.getCurrent(null).getCurrentUser(1).getUid(), receiver.getUid(), (ViewGroup) findViewById(R.id.rootView), recyclerView, database);
            m.setNotify(false);
            findViewById(R.id.tvFinalizarContrato).setVisibility(View.GONE);
        } else {
            RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT);

            recyclerView
                    .setLayoutParams(lp);

        }


        ((TextView) findViewById(R.id.tvReceiver)).setText(receiver.getUsername());
        CircleImageView ivPic = findViewById(R.id.ivPic);

        findViewById(R.id.buttonBack).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        if (receiver.getIsPro()) {

            findViewById(R.id.ContainerReceiver).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(SpecificChatActivity.this, ProfileActivity.class);
                    intent.putExtra("user", receiver);
                    startActivity(intent);
                }
            });

            if (!c.isFinished()) {
                findViewById(R.id.tvFinalizarContrato).setVisibility(View.VISIBLE);
                findViewById(R.id.tvFinalizarContrato).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        final CustomDialog dialog = new CustomDialog(SpecificChatActivity.this);
                        dialog.setTitle(getString(R.string.pudo_concretar));
                        dialog.setPositiveIcon(getResources().getDrawable(R.drawable.ic_check_black_24dp));
                        dialog.setNegativeIcon(getResources().getDrawable(R.drawable.ic_close_black_24dp));

                        dialog.setPositiveButton("", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                Intent intent = new Intent(SpecificChatActivity.this, UserReviewActivity.class);
                                intent.putExtra("user", receiver);
                                startActivity(intent);
                                m.finishChat();
                                finish();
                            }
                        });

                        dialog.setNegativeButton("", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                m.finishChat();
                                finish();
                            }
                        });
                        dialog.show();
                    }
                });
            } else {
                findViewById(R.id.tvFinalizarContrato).setVisibility(View.GONE);
            }
        } else {

            findViewById(R.id.tvFinalizarContrato).setVisibility(View.GONE);
        }

        String chatID = getIntent().getStringExtra("chatid");
        if (receiver.getHasPic()) {
            StorageReference storage = FirebaseStorage.getInstance().getReference();

            StorageReference userRef = storage.child(Constants.FIREBASE_USERS_PRO_CONTAINER_NAME + "/" + receiver.getUid() + receiver.getImgVersion() + ".jpg");
            GlideApp.with(this)
                    .load(userRef)
                    .into(ivPic);
        } else {
            ivPic.setImageDrawable(getResources().getDrawable(R.drawable.profile_pic));
        }

        final DatabaseReference messagesRef = Database
                .getDatabase()
                .getReference(Constants.FIREBASE_MESSAGES_CONTAINER_NAME)
                .child(chatID);

        recyclerViewAdapter = new ListViewChatsAdapter(String.class, 0, ViewHolderChats.class, messagesRef);
        recyclerView.setLayoutManager(new LinearLayoutManager(SpecificChatActivity.this));
        recyclerView.setAdapter(recyclerViewAdapter);
        loadingScreen.hide();
        recyclerViewAdapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onItemRangeInserted(int positionStart, int itemCount) {
                recyclerView.smoothScrollToPosition(recyclerViewAdapter.getItemCount());
            }
        });

        final View activityRootView = findViewById(R.id.rootView);
        activityRootView.getViewTreeObserver().
                addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        int heightDiff = activityRootView.getRootView().getHeight() - activityRootView.getHeight();
                        if (heightDiff > dpToPx(getApplication(), 200)) { // if more than 200 dp, it's probably a keyboard...
                            recyclerView.smoothScrollToPosition(recyclerViewAdapter.getItemCount());
                        }
                    }
                });

        if (!c.isFinished()) {
            database.child(Constants.FIREBASE_CHATS_CONTAINER_NAME)
                    .child(HolderCurrentAccountManager.getCurrent(null).getCurrentUser(1).getUid())
                    .child(receiver.getUid())
                    .addValueEventListener(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            ChatOverview c = dataSnapshot.getValue(ChatOverview.class);
                            if (c != null && c.isFinished()) {
                                finish();
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {

                        }
                    });
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        isRunning = true;
    }

    @Override
    protected void onStop() {
        super.onStop();
        isRunning = false;
    }

    @Override
    public void onBackPressed() {
        finish();
    }

    private class ListViewChatsAdapter extends FirebaseRecyclerAdapter<String, ViewHolderChats> {
        private Gson gson;
        private LayoutInflater inflater;

        ListViewChatsAdapter(Class<String> modelClass, int modelLayout, Class<ViewHolderChats> viewHolderClass, Query ref) {
            super(modelClass, modelLayout, viewHolderClass, ref);
            gson = GsonerMessages.getGson();
        }

        @NonNull
        @Override
        public ViewHolderChats onCreateViewHolder(ViewGroup parent, int viewType) {
            inflater = LayoutInflater.from(parent.getContext());
            switch (viewType) {
                case 0: //received message, previous one same person
                    return new ViewHolderChats(inflater.inflate(R.layout.message_received_cont, parent, false));
                case 1: //received message, previous one other person
                    return new ViewHolderChats(inflater.inflate(R.layout.message_received_change, parent, false));
                case 2: //sent message, previous one same person
                    return new ViewHolderChats(inflater.inflate(R.layout.message_sent_cont, parent, false));
                case 4:
                    return new ViewHolderChats(inflater.inflate(R.layout.message_contract_finished, parent, false));
                default: //sent message, previous one is user
                    return new ViewHolderChats(inflater.inflate(R.layout.message_sent_change, parent, false));
            }
        }

        @Override
        protected void populateViewHolder(ViewHolderChats holder, String str, int position) {
            Message msg = gson.fromJson(str, Message.class);

            msg.fillHolder(inflater.getContext(), holder);

            holder.setTimeStamp(msg.getTimeStamp());
        }

        @Override
        public int getItemViewType(int position) {
            Message msg = gson.fromJson(getItem(position), Message.class);

            String authorUID = msg.getAuthor();

            if (msg instanceof MessageContractFinished) {
                return 4;
            }

            Message last = null;
            if (position > 0) {
                last = gson.fromJson(getItem(position - 1), Message.class);
            }
            if (authorUID.equals(receiver.getUid())) {
                if (position > 0 && last.getAuthor().equals(receiver.getUid())) {
                    return 0;
                } else {
                    return 1;
                }
            } else {
                if (position > 0 && !last.getAuthor().equals(receiver.getUid())) {
                    return 2;
                } else {
                    return 3;
                }
            }
        }
    }
}