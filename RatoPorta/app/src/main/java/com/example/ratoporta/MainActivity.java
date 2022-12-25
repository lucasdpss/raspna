package com.example.ratoporta;

import androidx.appcompat.app.AppCompatActivity;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import com.rabbitmq.client.AlreadyClosedException;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.Channel;
import java.io.IOException;
import java.util.concurrent.TimeoutException;

public class MainActivity extends AppCompatActivity {
    //Definicoes da RabbitMQ
    private String QUEUE_NAME = "rpirelay";
    private String ROUTING_KEY = "rpirelay";
    private String EXCHANGE_NAME = "rpimessage";
    private ConnectionFactory factory;
    private Connection connection;
    private Channel channel;
    private PublishThread publishThread;
    private ConnectThread connectThread;
    private boolean isConnected = false;
    private boolean tryingToConnect = false;

    //Definicao do botao
    private Button button;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getSupportActionBar().hide(); //esconde a barra de titulo

        //Thread de conexao
        tryingToConnect = true;
        isConnected = false;
        connectThread = new ConnectThread();
        connectThread.start();

        //Configura botao de conectar
        button = (Button) findViewById(R.id.button_open_door);
        button.setEnabled(false);
        button.setBackgroundTintList(ColorStateList.valueOf((getResources().getColor(R.color.gray_disabled))));
        button.setAlpha(0.96f);
        button.setText("Conectando...");
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if(connection != null && connection.isOpen() && channel != null && channel.isOpen()){
                    publishThread = new PublishThread();
                    publishThread.start();
                }else{
                    if(!tryingToConnect){
                        tryingToConnect = true;
                        isConnected = false;

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                button.setText("Conectando...");
                                button.setEnabled(false);
                                button.setBackgroundTintList(ColorStateList.valueOf((getResources().getColor(R.color.gray_disabled))));
                            }
                        });

                        connectThread = new ConnectThread();
                        connectThread.start();
                    }
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

    @Override
    protected void onResume() {
        super.onResume();
        if(!isConnected && !tryingToConnect){
            tryingToConnect = true;
            isConnected = false;

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    button.setText("Conectando...");
                    button.setEnabled(false);
                    button.setBackgroundTintList(ColorStateList.valueOf((getResources().getColor(R.color.gray_disabled))));

                }
            });

            connectThread = new ConnectThread();
            connectThread.start();
        }

    }

    class ConnectThread extends Thread {
        @Override
        public void run(){
            factory = new ConnectionFactory();
            factory.setHost("localhost");
            factory.setUsername("guest");
            factory.setPassword("guest");

            try {
                connection = factory.newConnection();
                channel = connection.createChannel();
                channel.exchangeDeclare(EXCHANGE_NAME, "direct", false);  //mudar no raspi para "direc" tbm
                channel.queueDeclare(QUEUE_NAME, false, false, false, null);
                channel.queueBind(QUEUE_NAME, EXCHANGE_NAME, ROUTING_KEY);
                isConnected = true;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        button.setText("Abrir porta");
                        button.setEnabled(true);
                        button.setBackgroundTintList(ColorStateList.valueOf((getResources().getColor(R.color.verde_rep))));
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        button.setText("Sem conexão :(");
                        button.setEnabled(false);
                        button.setBackgroundTintList(ColorStateList.valueOf((getResources().getColor(R.color.gray_disabled))));
                    }
                });
            }
            tryingToConnect = false;
        }
    }

    class PublishThread extends Thread {
        @Override
        public void run(){
            try {
                String message = "on";
                channel.basicPublish(EXCHANGE_NAME, ROUTING_KEY, null, message.getBytes());
                System.out.println(" [x] Sent '" + message + "'");


                //Delay para habilitar o botao novamente
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        button.setText("Porta aberta");
                        button.setEnabled(false);
                        button.setBackgroundTintList(ColorStateList.valueOf((getResources().getColor(R.color.gray_disabled))));
                    }
                });

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        final Handler handler = new Handler();
                        handler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                // Faca depois de 4s
                                button.setText("Abrir porta");
                                button.setEnabled(true);
                                button.setBackgroundTintList(ColorStateList.valueOf((getResources().getColor(R.color.verde_rep))));
                            }
                        }, 4000);
                    }
                });
            } catch (IOException e) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        button.setText("Não Abriu :(");
                        button.setEnabled(false);
                        button.setBackgroundTintList(ColorStateList.valueOf((getResources().getColor(R.color.verde_rep))));
                    }
                });
                System.out.println("Rabbitmq problem");
            }

        }
    }

}