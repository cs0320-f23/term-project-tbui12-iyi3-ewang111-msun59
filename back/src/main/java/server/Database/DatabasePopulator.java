package server.Database;

// import com.google.api.core.ApiFuture;
// import com.google.api.core.ApiFutureCallback;
// import com.google.api.core.ApiFutures;
// import com.google.auth.oauth2.GoogleCredentials;
// import com.google.cloud.firestore.DocumentReference;
// import com.google.cloud.firestore.Firestore;
// import com.google.cloud.firestore.WriteResult;
// import com.google.common.util.concurrent.MoreExecutors;
// import com.google.firebase.FirebaseApp;
// import com.google.firebase.FirebaseOptions;
// import com.google.firebase.cloud.FirestoreClient;

import com.google.api.core.ApiFuture;
import com.google.api.core.ApiFutureCallback;
import com.google.api.core.ApiFutures;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.WriteResult;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.cloud.FirestoreClient;
import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import me.tongfei.progressbar.ProgressBar;
import okio.Buffer;
import server.Exceptions.DatasourceException;
import server.Handlers.DrugResponse;
import server.Handlers.DrugResponse.ActiveIngredient;
import server.Handlers.DrugResponse.Result;
import server.Handlers.LabelResponse;

public class DatabasePopulator {

  private static Firestore db;
  private Map<String, Set<String>> ndc_to_ingredients;
  private Map<String, Set<String>> active_ingredient_to_ndc;
  private Map<String, Result> ndc_to_result;
  private DrugResponse drugResponse;
  private JsonAdapter<DrugResponse> drugsFeatureAdapter;

  public DatabasePopulator() throws IOException, DatasourceException {

    FileInputStream serviceAccount = new FileInputStream("data/private/clearmeds_private_key.json");

    FirebaseOptions options =
        new FirebaseOptions.Builder()
            .setCredentials(GoogleCredentials.fromStream(serviceAccount))
            .build();

    FirebaseApp.initializeApp(options);

    this.db = FirestoreClient.getFirestore();

    Moshi moshi = new Moshi.Builder().build();

    this.drugsFeatureAdapter = moshi.adapter(DrugResponse.class);

    Path jsonFilePath = Path.of("data/drug-drugsfda-0001-of-0001.json");
    String jsonData = new String(Files.readAllBytes(jsonFilePath));
    this.drugResponse = this.drugsFeatureAdapter.fromJson(jsonData);

    //    this.ndc_to_ingredients = new HashMap<String, Set<String>>();
    //    this.active_ingredient_to_ndc = new HashMap<String, Set<String>>();
    //    this.ndc_to_result = new HashMap<String, Result>();

    this.parse();

    //    System.out.println(this.ndc_to_result.values());
    this.addToDatabase();
  }

  private void parse() {

    int processors = Runtime.getRuntime().availableProcessors();
    int poolSize =
        Math.min(
            processors * 2,
            this.drugResponse.results().size()); // Choose a multiple or other suitable value
    poolSize = 2;
    ScheduledExecutorService executorService = Executors.newScheduledThreadPool(poolSize);

    //    ProgressBar pb = new ProgressBar("Test", this.drugResponse.results().size());

    int batchSize = 1000;

    ProgressBar pb =
        new ProgressBar(
            "parsing", (long) Math.ceil(this.drugResponse.results().size() / batchSize));
    pb.start();

    for (int b = 0; b < Math.ceil(this.drugResponse.results().size() / batchSize); b++) {

      pb.step();

      for (int i = b; i < b + batchSize; i++) {
        if (i >= this.drugResponse.results().size()) {
          break;
        }

        //      }
        //    }

        //    for (int i = 1000; i < 2000; i++) {
        // un commet this out when done to do the whole
        //    for (Result result : this.drugResponse.results()) {
        Result result = this.drugResponse.results().get(i);

        // null checking to ensure if openfda and product_ndc exists. if it doesn't, skip it.
        if (result.openFDA() == null || result.openFDA().product_ndc() == null) {
          continue;
        }

        List<String> product_ndcs = result.openFDA().product_ndc();

        // get the active_ingredients for the result (the active ingredients should be shared among
        // the all the ndcs for this result object
        //      Set<String> active_ingredients = new HashSet<String>();
        //      for (ActiveIngredient active_ingredient :
        // result.products().get(0).active_ingredients()) {
        //        active_ingredients.add(active_ingredient.name());
        //      }
        //      Set<String> active_ingredients =
        // result.products().get(0).active_ingredients().stream()
        //            .map(ActiveIngredient::name)
        //            .collect(Collectors.toSet());

        Set<String> active_ingredients;

        if (result.products() != null && !result.products().isEmpty()) {
          active_ingredients =
              result.products().get(0).active_ingredients().stream()
                  .map(ActiveIngredient::name)
                  .collect(Collectors.toSet());
        } else {
          active_ingredients = Collections.emptySet();
        }

        // for every ndc in product_ndcs, get the list of all ingredients (active and inactive)
        for (String ndc : product_ndcs) {
          // create a list of all the ingredients, initalized with the active ingredietns
          Set<String> ingredients = new HashSet<String>(active_ingredients);

          executorService.schedule(
              () -> this.process_ndc(ndc, active_ingredients, ingredients, result),
              5,
              TimeUnit.SECONDS);
        }
      }

      System.out.println("finished batch: " + b);
      try {
        Thread.sleep(150000);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }
    executorService.shutdown();
    try {
      executorService.awaitTermination(20, TimeUnit.MINUTES);
    } catch (InterruptedException e) {
      // Handle interruption
      System.out.println();
      e.printStackTrace();
    }

    pb.stop();
  }

  private void process_ndc(
      String ndc, Set<String> active_ingredients, Set<String> ingredients, Result result) {

    try {

      StringBuilder urlBuilder =
          new StringBuilder()
              .append("https://api.fda.gov/drug/label.json?search=openfda.product_ndc:\"")
              .append(ndc)
              .append("\"&limit=1");

      URL requestURL = new URL("https", "api.fda.gov", urlBuilder.toString());

      HttpURLConnection clientConnection = connect(requestURL);
      Moshi moshi = new Moshi.Builder().build();
      JsonAdapter<LabelResponse> adapter = moshi.adapter(LabelResponse.class);
      LabelResponse labelResponse =
          adapter.fromJson(new Buffer().readFrom(clientConnection.getInputStream()));

      if (labelResponse.results() != null) {
        for (LabelResponse.Result res : labelResponse.results()) {
          if (res.active_ingredient() != null) {
            ingredients.add(res.active_ingredient().toString());
          }
          if (res.inactive_ingredient() != null) {
            ingredients.add(res.inactive_ingredient().toString());
          }
        }
      }

    } catch (IOException e) {
      System.out.println();
      System.out.println(e.getMessage());
      e.printStackTrace();
    } catch (Exception e) {
      System.err.println();
      System.err.println(e);
      e.printStackTrace();
    }

    // this should be one to one / immutable (or will not be edited after doing it once)
    this.ndc_to_result.put(ndc, result);
    this.ndc_to_ingredients.put(ndc, ingredients);

    // this adds the current ndc to the set of active_ingredients to ndc (need to check if it exists
    // and add to it first, before writing)
    for (String a_i : active_ingredients) {
      if (!this.active_ingredient_to_ndc.containsKey(a_i)) {
        this.active_ingredient_to_ndc.put(a_i, new HashSet<String>());
      }
      this.active_ingredient_to_ndc.get(a_i).add(ndc);
    }
  }

  /**
   * Private helper method; throws IOException so different callers can handle differently if
   * needed.
   *
   * @param requestURL
   * @return
   * @throws IOException
   */
  private static HttpURLConnection connect(URL requestURL) throws IOException {
    URLConnection urlConnection = requestURL.openConnection();
    if (!(urlConnection instanceof HttpURLConnection))
      throw new IOException("Unexpected: result of connection wasn't HTTP");
    HttpURLConnection clientConnection = (HttpURLConnection) urlConnection;
    clientConnection.connect(); // GET
    if (clientConnection.getResponseCode() != 200) {
      throw new IOException(
          "Unexpected: API connection not success status: "
              + clientConnection.getResponseMessage()
              + ". Request api call: "
              + requestURL);
    }

    return clientConnection;
  }

  public static void main(String args[]) {
    //    CollectionReference collection = db.collection("ndc_to_ingredients");
    //    System.out.println(collection);
    try {

      DatabasePopulator database = new DatabasePopulator();
    } catch (IOException e) {
      e.printStackTrace();
    } catch (DatasourceException e) {
      e.printStackTrace();
    }
  }

  private void addToDatabase() {
    // stores the hashmaps
    storeMapInFirestore("ndc_to_ingredients", ndc_to_ingredients);
    storeMapInFirestore("active_ingredient_to_ndc", active_ingredient_to_ndc);
    storeResultMapInFirestore("ndc_to_result", ndc_to_result);
  }

  private void storeMapInFirestore(String collectionName, Map<String, Set<String>> map) {
    for (Map.Entry<String, Set<String>> entry : map.entrySet()) {

      // adding data to the document
      String key = entry.getKey();
      Set<String> values = entry.getValue();
      try {

        DocumentReference docRef = db.collection(collectionName).document(key);
        ArrayList<String> valueList = new ArrayList<>();
        //        ArrayList<String> valueList = new ArrayList<>(values);
        valueList.addAll(values);
        docRef.set(Map.of("values", valueList));

        System.out.println("Document added with ID: " + key);
      } catch (Exception e) {
        // array index out of bounds excpetion for some reason
        e.printStackTrace();
        System.out.println(values);
      }
    }
  }

  private void storeResultMapInFirestore(String collectionName, Map<String, Result> resultMap) {
    CountDownLatch latch = new CountDownLatch(resultMap.size());

    for (Map.Entry<String, Result> entry : resultMap.entrySet()) {
      String key = entry.getKey();
      Result result = entry.getValue();

      Map<String, Object> resultData = convertResultToMap(result);

      DocumentReference docRef = db.collection(collectionName).document(key);

      ApiFuture<WriteResult> future = docRef.set(resultData);

      ApiFutures.addCallback(
          future,
          new ApiFutureCallback<>() {
            @Override
            public void onSuccess(WriteResult writeResult) {
              System.out.println("Document added with ID: " + key);
              latch.countDown();
            }

            @Override
            public void onFailure(Throwable throwable) {
              System.err.println("Error adding document: " + throwable);
              latch.countDown();
            }
          },
          MoreExecutors.directExecutor());
    }

    try {
      // Wait for all operations to complete
      latch.await();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      System.err.println("Thread interrupted while waiting for Firestore operations to complete.");
    }
  }

  private Map<String, Object> convertResultToMap(DrugResponse.Result result) {
    Map<String, Object> resultData = new HashMap<>();

    resultData.put("application_number", result.application_number());
    resultData.put("sponsor_name", result.sponsor_name());

    // Convert nested objects to Maps
    List<Map<String, Object>> productDataList = new ArrayList<>();
    for (DrugResponse.Product product : result.products()) {
      Map<String, Object> productData = new HashMap<>();
      // Add relevant fields from the Product class
      productData.put("product_number", product.product_number());
      productData.put("reference_drug", product.reference_drug());
      productData.put("brand_name", product.brand_name());

      // Convert active ingredients to a list of maps
      List<Map<String, Object>> activeIngredientsDataList = new ArrayList<>();
      for (DrugResponse.ActiveIngredient activeIngredient : product.active_ingredients()) {
        Map<String, Object> activeIngredientData = new HashMap<>();
        activeIngredientData.put("name", activeIngredient.name());
        activeIngredientData.put("strength", activeIngredient.strength());
        activeIngredientsDataList.add(activeIngredientData);
      }
      productData.put("active_ingredients", activeIngredientsDataList);

      // Add other fields as needed
      productDataList.add(productData);
    }
    resultData.put("products", productDataList);

    // Similarly, convert the 'openFDA' field to a Map
    Map<String, Object> openFDAData = new HashMap<>();
    // Add relevant fields from the OpenFDA class
    openFDAData.put("application_number", result.openFDA().application_number());
    openFDAData.put("brand_name", result.openFDA().brand_name());
    openFDAData.put("generic_name", result.openFDA().generic_name());
    // Add other fields as needed
    resultData.put("openfda", openFDAData);

    return resultData;
  }

  public Map<String, Set<String>> getNdc_to_ingredients() {
    return this.ndc_to_ingredients;
  }

  public Map<String, Set<String>> getActive_ingredient_to_ndc() {
    return this.active_ingredient_to_ndc;
  }

  public Map<String, Result> getNdc_to_result() {
    return this.ndc_to_result;
  }
}