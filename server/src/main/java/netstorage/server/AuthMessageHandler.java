package netstorage.server;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.ReferenceCountUtil;
import messages.AuthMsg;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;

public class AuthMessageHandler extends ChannelInboundHandlerAdapter {

    private boolean isAuthorized;
    private String clientName;

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {

        System.out.println("AuthMessageHandler received message!");
        if (msg == null)
            return;
        else {
            if (!isAuthorized) {

                if (msg instanceof AuthMsg) {
                    AuthMsg am = (AuthMsg) msg;

                    clientName = CheckLoginPass(am.getLogin(),am.getPassword());

                    if (clientName != null) {
                        isAuthorized = true;
                        ctx.fireChannelRead(new AuthMsg(clientName));

                        ReferenceCountUtil.release(msg);
                    }
                } else {
                    ReferenceCountUtil.release(msg);
                }
            } else {
                ctx.fireChannelRead(msg);
            }
        }
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) {
        ctx.flush();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        super.exceptionCaught(ctx, cause);
    }

    private String CheckLoginPass(String login, String pass){

        String url = "jdbc:mysql://localhost:3306/net_storage?useUnicode=true&useJDBCCompliantTimezoneShift=true&useLegacyDatetimeCode=false&serverTimezone=UTC";
        String user = "net_storage_user";
        String password = "Net_storage";

        Connection con = null;
        PreparedStatement preparedStatement = null;
        ResultSet rs = null;

        clientName = null;

        try {

        Class.forName("com.mysql.cj.jdbc.Driver");

        // opening database connection to MySQL server
         con = DriverManager.getConnection(url, user, password);

         preparedStatement = con.prepareStatement(
                    "SELECT clientname FROM users where login = ? and password = ?");

         preparedStatement.setString(1, login);
         preparedStatement.setString(2, pass);

             rs = preparedStatement.executeQuery();

            while (rs.next()) {
                clientName = rs.getString("clientname") ;
            }

            return clientName;

        } catch (SQLException | ClassNotFoundException sqlEx) {
            sqlEx.printStackTrace();
            return clientName;
        } finally {
            //close connection ,stmt and resultset here
            try { con.close(); } catch(SQLException se) { /*can't do anything */ }
            try { preparedStatement.close(); } catch(SQLException se) { /*can't do anything */ }
            try { rs.close(); } catch(SQLException se) { /*can't do anything */ }
        }


    }

}
