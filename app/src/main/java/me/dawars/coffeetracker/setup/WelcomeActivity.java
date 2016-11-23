package me.dawars.coffeetracker.setup;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.util.Pair;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;
import me.dawars.coffeetracker.R;

public class WelcomeActivity extends AppCompatActivity {

    @Bind(R.id.logo_holder)
    View mLogoHolder;
    @Bind(R.id.coffee_logo)
    View mLogo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);
        ButterKnife.bind(this);
    }

    @OnClick(R.id.welcome_setup_btn)
    public void setup() {

        String transitionLogo = getString(R.string.transition_logo);
        String transitionLogoHolder = getString(R.string.transition_logo_holder);

        ActivityOptionsCompat options =
                ActivityOptionsCompat.makeSceneTransitionAnimation(this,
                        Pair.create(mLogo, transitionLogo),
                        Pair.create(mLogoHolder, transitionLogoHolder)

                );
        Intent intent = new Intent(getApplicationContext(), PairActivity.class);
        ActivityCompat.startActivity(this, intent, options.toBundle());
    }

    @OnClick(R.id.welcome_learn_more)
    public void learnMore() {
        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.facebook.com/events/1090113621019803/")));
    }
}
