package com.youssefdirani.automation;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class AppDescription extends AppCompatActivity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.app_description);
    }

    @SuppressLint("SetTextI18n")
    @Override
    protected void onStart() {
        super.onStart();
        setTitle(getApplicationContext().getString(R.string.app_name) + " - " +
                getApplicationContext().getString(R.string.about_app));
        final TextView description_textView = findViewById(R.id.textView_description);
        description_textView.setMovementMethod(LinkMovementMethod.getInstance());

        description_textView.setText(Html.fromHtml("مرحباً" +
                "<br/><br/>" +
                "شكراً لاستخدامك تطبيق " +
                getApplicationContext().getString(R.string.app_name) +
                ".<br/>" +
                "أتمنى أن ينال إعجابك." +
                "<br/><br/>" +
                "هذا التطبيق تم تطويره بواسطتي، المهندس و خريج الدراسات العليا، يوسف ديراني." +
                "<br/>" +
                "التطوير لا يتعلّق فقط ببرمجة التطبيق على الهاتف، بل هو يشمل الحلول الذكية و الautomation." +
                "<br/><br/>" +
                "أنا أتمنى منكم أن تتابعوا أخباري باستمرار، فهذا يعطيني حماساً و اندفاعاً." +
                "<br/>" +
                "لمعرفة بعض الإنجازات يمكنكم زيارة " +
                "<a href='https://vimeo.com/user60245379'>هذا الموقع</a>" +
                " و هناك يمكنكم مشاهدة الفيديوهات التي أحمّلها. (تلك القناة قد تتغيّر من دون إشعار.)<br/>" +
                "تفضلوا بزيارتي في النميرية، طريق عام الشرقية، بناية الندى 1، محل إلكروتل ديراني. " +
                " (يمكنكم رؤية الموقع بواسطة " +
                "<a href='https://google.com/maps/@33.4165429,35.41302,19z'>الخريطة</a>" +
                ".)" +
                "<br/><br/>" +
                "لأي مساعدة تقنية، لطفاً إتصل على 70/853721." +
                "<br/><br/>" +
                "أي فكرة تعاونية، تعليق، أو اقتراح سيكون مرحّباً به تماماً.<br/>" +
                "معاً سوف نسعى لنصل لحالتنا الفُضلى الطبيعية إن شاء الله.<br/>"));

        final View backToWelcome_Button = findViewById(R.id.button_backToWelcomeFromDescription);
        backToWelcome_Button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //startActivity(new Intent(getApplicationContext(), readButton.WelcomeActivity.class));
                finish();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
    }
}
