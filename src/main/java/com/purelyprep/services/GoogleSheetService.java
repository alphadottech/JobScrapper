package com.purelyprep.services;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.SheetsScopes;
import com.google.api.services.sheets.v4.model.ValueRange;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Collections;
import java.util.List;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.net.URISyntaxException;

@Service
public class GoogleSheetService {

    private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();
    private static final String APPLICATION_NAME = "PurelyPrep";

    private static final Logger log = LoggerFactory.getLogger(GoogleSheetService.class);

    private static final List<String> SCOPES = Collections.singletonList(SheetsScopes.SPREADSHEETS);

    @Value("${google.sheets.service.email}")
    private String serviceAccountEmail;

    final File googleSheetsKeyFile;

    public GoogleSheetService(@Qualifier("GoogleSheetsKey") File googleSheetsKeyFile) {
        this.googleSheetsKeyFile = googleSheetsKeyFile;
    }

    /**
     * Creates an authorized Credential object.
     * @return An authorized Credential object.
     * @throws IOException If there is no client_secret.
     */
    private Credential getCredentials() throws URISyntaxException, IOException, GeneralSecurityException {
        return new GoogleCredential.Builder()
            .setTransport(GoogleNetHttpTransport.newTrustedTransport())
            .setJsonFactory(JSON_FACTORY)
            .setServiceAccountId(serviceAccountEmail)
            .setServiceAccountPrivateKeyFromP12File(googleSheetsKeyFile)
            .setServiceAccountScopes(SCOPES)
            .build();
    }

    // Eg: spreadsheetId: {from google sheets url}; range: Sheet1!A1:A1
    public List<List<Object>> readSheet(String spreadsheetId, String range) {
        try {
            final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
            Sheets service = new Sheets.Builder(HTTP_TRANSPORT, JSON_FACTORY, getCredentials())
                    .setApplicationName(APPLICATION_NAME)
                    .build();
            ValueRange response = service.spreadsheets().values()
                    .get(spreadsheetId, range)
                    .execute();
            return response.getValues();
        } catch(Exception ex) {
            log.error("Error reading sheet: ", ex);
            return Collections.emptyList();
        }
    }

    // Eg: spreadsheetId: {from google sheets url}; range: Sheet1!A1:A1
    public void addValue(String spreadsheetId, String range, Object value) {
        try {
            final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
            Sheets service = new Sheets.Builder(HTTP_TRANSPORT, JSON_FACTORY, getCredentials())
                    .setApplicationName(APPLICATION_NAME)
                    .build();
            ValueRange body = new ValueRange()
                    .setValues(List.of(Collections.singletonList(value)));
            service.spreadsheets().values().update(spreadsheetId, range, body)
                    .setValueInputOption("RAW") // RAW or USER_ENTERED (will infer type and treat, eg =1+2, as a formula
                    .execute();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

//    public static void main(String[] args) {
//        GoogleSheetService reader = new GoogleSheetService();
//        try {
//            reader.addValue("16hIn5BJUNfzsFRixdxwMa-0OgoRKIjrIV5grlJ1f73Y", "Sheet1!A1:A1", "foo-bar");
//        } catch (Exception e) {
//            log.error("Error: ", e);
//        }
//    }
}
