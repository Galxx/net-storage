package netstorage.server;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.ReferenceCountUtil;
import messages.AuthMsg;

import java.util.ArrayList;
import java.util.HashMap;

public class AuthMessageHandler extends ChannelInboundHandlerAdapter {

    private boolean isAuthorized;
    private String clientName;

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {

        //todo Заменить на обращение к БД
        String[][] userList = new String[2][3];
        userList[0][0] = "log1";
        userList[0][1] = "pass1";
        userList[0][2] = "Galxx";
        userList[1][0] = "log2";
        userList[1][1] = "pass2";
        userList[1][2] = "Dalxx";


        System.out.println("AuthMessageHandler received message!");
        if (msg == null)
            return;
        else {
            if (!isAuthorized) {

                if (msg instanceof AuthMsg) {
                    AuthMsg am = (AuthMsg) msg;

                    //todo Заменить на обращение к БД 2
                    clientName = null;
                    for (int i = 0; i < userList.length; i++) {
                        if ( userList[i][0].equals(am.getLogin()) && userList[i][1].equals(am.getPassword())) {
                            clientName = userList[i][2];
                        }
                    }

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
}
