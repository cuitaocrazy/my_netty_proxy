import akka.actor.ActorRef
import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.socket.SocketChannel
import io.netty.channel._
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.nio.NioServerSocketChannel
import io.netty.util.concurrent.{Future, GenericFutureListener}

import scala.util.{Success, Failure, Try}

object WeixinClientProxyServer {
  private final val port = 1235
  private final val host = "0.0.0.0"

  def start(actorRef: ActorRef) = {
    val boss = new NioEventLoopGroup(1)
    val work = new NioEventLoopGroup()

    def shutdown() = {
      boss.shutdownGracefully()
      work.shutdownGracefully()
    }

    Try {
      new ServerBootstrap().group(boss, work)
        .channel(classOf[NioServerSocketChannel])
        .option(ChannelOption.SO_BACKLOG, Int.box(256))
        .childHandler(new ChannelInitializer[SocketChannel] {
          override def initChannel(ch: SocketChannel): Unit = {
            ch.pipeline().addLast(new WeixinClientProxyHandler(actorRef))
          }
        }).bind(host, port).sync().channel().closeFuture().addListener(new GenericFutureListener[Future[Void]] {
        override def operationComplete(future: Future[Void]): Unit = {
          shutdown()
        }
      })
    } match {
      case f @ Failure(_) =>
        shutdown()
        f
      case s @ Success(_) => s
    }
  }
}

class WeixinClientProxyHandler(actorRef: ActorRef) extends ChannelInboundHandlerAdapter {
  override def channelActive(ctx: ChannelHandlerContext): Unit = {
    val channel = ctx.channel()
    channel.closeFuture().addListener(new GenericFutureListener[Future[Void]] {
      override def operationComplete(future: Future[Void]): Unit = {
        actorRef ! ClientSocketChannelCloseCmd(channel)
      }
    })
    actorRef ! ClientSocketChannelCreatedCmd(channel)
  }
}