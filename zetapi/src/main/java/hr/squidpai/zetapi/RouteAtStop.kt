package hr.squidpai.zetapi

/** Class containing information about a route that passes through a stop. */
public class RouteAtStop internal constructor() {

   /** Is this the first stop for most trips? */
   public var isFirst: Boolean = true
      internal set

   /** Is this the last stop for most trips? */
   public var isLast: Boolean = true
      internal set

   /** Does this route stop when traveling in [DirectionId.Zero]? */
   public var stopsAtDirectionZero: Boolean = false
      internal set

   /** Does this route stop when traveling in [DirectionId.One]? */
   public var stopsAtDirectionOne: Boolean = false
      internal set
}
