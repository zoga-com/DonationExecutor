package igorlink.donationexecutor.playersmanagement.donationalerts;

import igorlink.donationexecutor.DonationExecutor;
import igorlink.donationexecutor.playersmanagement.Donation;
import igorlink.service.Utils;
import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitRunnable;
import org.json.JSONException;
import org.json.JSONObject;
import java.net.URI;
import java.net.URISyntaxException;
import static igorlink.service.Utils.logToConsole;

public class DonationAlertsConnection {
    private static final String DASERVER = "https://socket.donationalerts.ru:443";
    private final Socket socket;
    private final DonationAlertsToken donationAlertsToken;


    public DonationAlertsConnection(DonationAlertsToken donationAlertsToken) throws URISyntaxException {
        this.donationAlertsToken = donationAlertsToken;
        URI url = new URI(DASERVER);
        socket = IO.socket(url);

        Emitter.Listener connectListener = (Object... arg0) -> {
            socket.emit("add-user", new JSONObject()
                    .put("token", donationAlertsToken.getToken())
                    .put("type", "minor"));
            logToConsole("Произведено успешное подключение для токена §b" + donationAlertsToken.getToken());
        };
        Emitter.Listener disconectListener = (Object... arg0) -> logToConsole("Произведено отключение для токена §b" + donationAlertsToken.getToken());
        Emitter.Listener errorListener = (Object... arg0) -> logToConsole("Произошла ошибка подключения к Donation Alerts!");

        Emitter.Listener donationListener = (Object... arg0) -> {
            JSONObject json = new JSONObject((String) arg0[0]);
            //logToConsole((String) arg0[0]);
            new BukkitRunnable() {
                @Override
                public void run() {

                    if ((json.isNull("username")) || (json.isNull("amount_formatted"))) {
                        return;
                    }

                    try {
                        if ( ((json.getString("amount_formatted")).length() == 0) || (Utils.cutOffKopeykis(json.getString("amount_formatted")).equals("0")) ) {
                            return;
                        }
                    } catch (JSONException e) {
                        return;
                    }

                    DonationAlertsConnection.this.donationAlertsToken.addToDonationsQueue(new Donation(Bukkit.getConsoleSender(),
                            json.getString("username"),
                            json.getString("amount_formatted"),
                            json.getString("message")));

                }
            }.runTask(DonationExecutor.getInstance());
        };


        socket.on(Socket.EVENT_CONNECT, connectListener)
                .on(Socket.EVENT_DISCONNECT, disconectListener)
                .on(Socket.EVENT_ERROR, errorListener)
                .on("donation", donationListener);

    }


    public void connect() throws JSONException {
        socket.connect();
    }

    public void disconnect() throws JSONException {
        socket.disconnect();
    }

    public boolean getConnected() {
        return socket.connected();
    }


}