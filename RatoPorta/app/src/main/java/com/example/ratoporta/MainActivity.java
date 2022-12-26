package com.example.ratoporta;

import android.annotation.SuppressLint;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.os.Handler;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import com.rabbitmq.client.AlreadyClosedException;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeoutException;

public class MainActivity extends AppCompatActivity {
    private final String ROUTING_KEY = "rpirelay";
    private final String EXCHANGE_NAME = "rpimessage";
    private Connection connection;
    private Channel channel;
    private PublishThread publishThread;
    private ConnectThread connectThread;
    private boolean isConnected = false;
    private boolean tryingToConnect = false;

    //Definicao do botao
    private Button button;

    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Objects.requireNonNull(getSupportActionBar()).hide(); //esconde a barra de titulo

        //Thread de conexao
        tryingToConnect = true;
        isConnected = false;
        connectThread = new ConnectThread();
        connectThread.start();

        //Configura botao de conectar
        button = findViewById(R.id.button_open_door);
        button.setEnabled(false);
        button.setBackgroundTintList(ColorStateList.valueOf((getResources().getColor(R.color.gray_disabled))));
        button.setAlpha(0.96f);
        button.setText("Conectando...");
        button.setOnClickListener(v -> {
            if(connection != null && connection.isOpen() && channel != null && channel.isOpen()){
                publishThread = new PublishThread();
                publishThread.start();
            }else{
                if(!tryingToConnect){
                    tryingToConnect = true;
                    isConnected = false;

                    runOnUiThread(() -> {
                        button.setText("Conectando...");
                        button.setEnabled(false);
                        button.setBackgroundTintList(ColorStateList.valueOf((getResources().getColor(R.color.gray_disabled))));
                    });

                    connectThread = new ConnectThread();
                    connectThread.start();
                }
            }
        });

    }

    @Override
    protected void onPause() {
        super.onPause();
        isConnected = false;
        tryingToConnect = false;
        try {
            if(channel != null && channel.isOpen())
                channel.close();
            if(connection != null && connection.isOpen())
                connection.close();
        } catch (IOException | TimeoutException | AlreadyClosedException e) {
            System.out.println("erro de IO ou timeout ou ja fechado");
            e.printStackTrace();
        } catch (Exception e){
            System.out.println("deu ruim de alguma outra forma");
            e.printStackTrace();
        }
    }

    @SuppressLint("SetTextI18n")
    @Override
    protected void onResume() {
        super.onResume();
        if(!isConnected && !tryingToConnect){
            tryingToConnect = true;

            runOnUiThread(() -> {
                button.setText("Conectando...");
                button.setEnabled(false);
                button.setBackgroundTintList(ColorStateList.valueOf((getResources().getColor(R.color.gray_disabled))));

            });

            connectThread = new ConnectThread();
            connectThread.start();
        }

    }

    class ConnectThread extends Thread {
        @SuppressLint("SetTextI18n")
        @Override
        public void run(){
            ConnectionFactory factory = new ConnectionFactory();
            factory.setHost("192.168.1.3");
            factory.setUsername("rasp");
            factory.setPassword("rissa");

            try {
                Map<String, Object> args = new HashMap<>();
                args.put("x-message-ttl", 2000);
                connection = factory.newConnection();
                channel = connection.createChannel();
                channel.exchangeDeclare(EXCHANGE_NAME, "direct", false);
                //Definicoes da RabbitMQ
                String QUEUE_NAME = "rpirelay";
                channel.queueDeclare(QUEUE_NAME, false, false, true, args);
                channel.queueBind(QUEUE_NAME, EXCHANGE_NAME, ROUTING_KEY);
                isConnected = true;
                runOnUiThread(() -> {
                    button.setText("Abrir porta");
                    button.setEnabled(true);
                    button.setBackgroundTintList(ColorStateList.valueOf((getResources().getColor(R.color.verde_rep))));
                });
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> {
                    button.setText("Sem conexão :(");
                    button.setEnabled(false);
                    button.setBackgroundTintList(ColorStateList.valueOf((getResources().getColor(R.color.gray_disabled))));
                });
            }
            tryingToConnect = false;
        }
    }

    class PublishThread extends Thread {
        @SuppressLint("SetTextI18n")
        @Override
        public void run(){
            try {
                String message = "on";
                channel.basicPublish(EXCHANGE_NAME, ROUTING_KEY, null, message.getBytes());
                System.out.println(" [x] Sent '" + message + "'");


                //Delay para habilitar o botao novamente
                runOnUiThread(() -> {
                    button.setText("Porta aberta");
                    button.setEnabled(false);
                    button.setBackgroundTintList(ColorStateList.valueOf((getResources().getColor(R.color.gray_disabled))));
                });

                runOnUiThread(() -> {
                    final Handler handler = new Handler();
                    handler.postDelayed(() -> {
                        // Faca depois de 4s
                        button.setText("Abrir porta");
                        button.setEnabled(true);
                        button.setBackgroundTintList(ColorStateList.valueOf((getResources().getColor(R.color.verde_rep))));
                    }, 4000);
                });
            } catch (IOException e) {
                runOnUiThread(() -> {
                    button.setText("Não Abriu :(");
                    button.setEnabled(false);
                    button.setBackgroundTintList(ColorStateList.valueOf((getResources().getColor(R.color.verde_rep))));
                });
                System.out.println("Rabbitmq problem");
            }

        }
    }

}