package br.com.lfdb.zup;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.text.Html;
import android.util.Log;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import br.com.lfdb.zup.base.BaseActivity;
import br.com.lfdb.zup.core.Constantes;
import br.com.lfdb.zup.core.ConstantesBase;
import br.com.lfdb.zup.domain.Usuario;
import br.com.lfdb.zup.service.FeatureService;
import br.com.lfdb.zup.service.LoginService;
import br.com.lfdb.zup.service.UsuarioService;
import br.com.lfdb.zup.util.FontUtils;
import br.com.lfdb.zup.validador.CpfValidador;
import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.Response;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.Background;
import org.androidannotations.annotations.Click;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.UiThread;
import org.androidannotations.annotations.ViewById;
import org.json.JSONObject;

@EActivity(R.layout.activity_cadastro) public class CadastroActivity extends BaseActivity {

  private static final int REQUEST_SOCIAL = 9876;

  @ViewById EditText nameField;
  @ViewById EditText passField;
  @ViewById EditText emailField;
  @ViewById EditText confirmPassField;
  @ViewById EditText documentField;
  @ViewById EditText phoneField;
  @ViewById EditText addressField;
  @ViewById EditText complField;
  @ViewById EditText cepField;
  @ViewById EditText neighborhoodField;
  @ViewById EditText cityField;
  @ViewById TextView addAccount;
  @ViewById TextView btnCancel;
  @ViewById TextView btnCreate;
  @ViewById TextView terms;

  @AfterViews void init() {
    loadTypeface();
    loadTerms();
    cityField.setOnEditorActionListener((v, actionId, event) -> {
      boolean handled = false;
      if (actionId == EditorInfo.IME_ACTION_GO) {
        registerIsValid();
        handled = true;
      }
      return handled;
    });
  }

  @Click void btnCancel() {
    finish();
  }

  @Click void btnCreate() {
    registerIsValid();
  }

  @Click void terms() {
    startActivity(new Intent(this, TermosDeUsoActivity.class));
  }

  @UiThread void loadTypeface() {
    btnCancel.setTypeface(FontUtils.getRegular(this));
    btnCreate.setTypeface(FontUtils.getRegular(this));
    nameField.setTypeface(FontUtils.getLight(this));
    passField.setTypeface(FontUtils.getLight(this));
    confirmPassField.setTypeface(FontUtils.getLight(this));
    emailField.setTypeface(FontUtils.getLight(this));
    documentField.setTypeface(FontUtils.getLight(this));
    phoneField.setTypeface(FontUtils.getLight(this));
    addressField.setTypeface(FontUtils.getLight(this));
    complField.setTypeface(FontUtils.getLight(this));
    cepField.setTypeface(FontUtils.getLight(this));
    neighborhoodField.setTypeface(FontUtils.getLight(this));
    cityField.setTypeface(FontUtils.getLight(this));
    addAccount.setTypeface(FontUtils.getLight(this));
    terms.setTypeface(FontUtils.getLight(this));
  }

  @UiThread void loadTerms() {
    terms.setText(Html.fromHtml(getString(R.string.termos_de_uso_cadastro)));
  }

  void registerIsValid() {
    clear();
    List<Integer> validadores = isValid();
    if (!validadores.isEmpty()) {
      focusLabels(validadores);
      return;
    }
    if (FeatureService.getInstance(this).isAnySocialEnabled()) {
      startActivityForResult(new Intent(this, RedesSociaisCadastroActivity.class), REQUEST_SOCIAL);
      return;
    }
    register();
  }

  private List<Integer> isValid() {
    List<Integer> campos = new ArrayList<>();
    String pass = passField.getText().toString().trim();
    String confirmPass = confirmPassField.getText().toString().trim();
    if (pass.isEmpty() || confirmPass.isEmpty() || !passField.equals(confirmPass)) {
      campos.add(passField.getId());
      campos.add(confirmPassField.getId());
    } else if (pass.length() < 6) {
      campos.add(passField.getId());
      Toast.makeText(this, getString(R.string.pass_length_message), Toast.LENGTH_SHORT).show();
    }
    for (Integer id : Arrays.asList(R.id.campoNome, R.id.campoEmail, R.id.campoCPF,
        R.id.campoTelefone, R.id.campoEndereco, R.id.campoCEP, R.id.campoBairro,
        R.id.campoCidade)) {
      if (((TextView) findViewById(id)).getText().toString().trim().isEmpty()) {
        campos.add(id);
      }
    }
    String document = documentField.getText().toString().trim();
    if (!CpfValidador.isValid(document.replace("-", "").replace(".", ""))) {
      campos.add(documentField.getId());
    }
    return campos;
  }

  private void register() {
    Usuario usuario = new Usuario();
    usuario.setBairro(neighborhoodField.getText().toString());
    usuario.setCep(cepField.getText().toString());
    usuario.setComplemento(complField.getText().toString());
    usuario.setCpf(documentField.getText().toString());
    usuario.setEmail(emailField.getText().toString());
    usuario.setEndereco(addressField.getText().toString());
    usuario.setNome(nameField.getText().toString());
    usuario.setTelefone(phoneField.getText().toString());
    usuario.setSenha(passField.getText().toString());
    usuario.setConfirmacaoSenha(confirmPassField.getText().toString());
    usuario.setCidade(cityField.getText().toString().trim());
    tasker(usuario);
  }

  @UiThread void focusLabels(List<Integer> campos) {
    for (Integer id : campos) {
      findViewById(id).setBackgroundResource(R.drawable.textbox_red);
    }
    Toast.makeText(this, getString(R.string.review_fields_message), Toast.LENGTH_LONG).show();
  }

  @UiThread void clear() {
    for (Integer id : Arrays.asList(R.id.campoNome, R.id.campoEmail, R.id.campoCPF,
        R.id.campoTelefone, R.id.campoEndereco, R.id.campoCEP, R.id.campoBairro, R.id.campoSenha,
        R.id.campoConfirmarSenha, R.id.campoCidade)) {
      findViewById(id).setBackgroundResource(R.drawable.textbox_bg);
    }
  }

  @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    if (requestCode == REQUEST_SOCIAL && resultCode == Activity.RESULT_OK) {
      register();
    }
  }

  @Override protected String getScreenName() {
    return getString(R.string.cadastro);
  }

  @Background void tasker(Usuario usuario) {
    ProgressDialog dialog = new ProgressDialog(CadastroActivity.this);
    dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
    dialog.setIndeterminate(true);
    dialog.setMessage(getString(R.string.waiting_message));
    dialog.show();
    String result = null;
    try {
      SharedPreferences prefs =
          PreferenceManager.getDefaultSharedPreferences(CadastroActivity.this);
      GoogleCloudMessaging gcm = GoogleCloudMessaging.getInstance(CadastroActivity.this);
      prefs.edit()
          .putString("gcm", gcm.register(CadastroActivity.this.getString(R.string.gcm_project)))
          .apply();

      RequestBody body = RequestBody.create(MediaType.parse("application/json; charset=utf-8"),
          new UsuarioService().converterParaJSON(usuario).toString());
      Request request =
          new Request.Builder().url(Constantes.REST_URL + "/users").post(body).build();
      Response response = ConstantesBase.OK_HTTP_CLIENT.newCall(request).execute();
      if (response.isSuccessful()) {
        body = RequestBody.create(MediaType.parse("application/json; charset=utf-8"),
            new UsuarioService().loginData(usuario.getEmail(), usuario.getSenha(), PreferenceManager
                .getDefaultSharedPreferences(CadastroActivity.this)
                .getString("gcm", "")));
        request =
            new Request.Builder().url(Constantes.REST_URL + "/authenticate").post(body).build();
        response = ConstantesBase.OK_HTTP_CLIENT.newCall(request).execute();
        if (response.isSuccessful()) {
          result = response.body().string();
        } else {
          Log.e("Error!", response.body().toString());
        }
      }
      dialog.dismiss();
      if (result == null) {
        Toast.makeText(CadastroActivity.this, getString(R.string.register_fail), Toast.LENGTH_LONG)
            .show();
        return;
      }
      JSONObject json = new JSONObject(result);
      new LoginService().registrarLogin(CadastroActivity.this, json.getJSONObject("user"),
          json.getString("token"));
      Toast.makeText(CadastroActivity.this, getString(R.string.login_success), Toast.LENGTH_LONG)
          .show();
      setResult(Activity.RESULT_OK);
      finish();
    } catch (Exception e) {
      Log.e("ZUP Register error", e.getMessage());
    }
  }
}
