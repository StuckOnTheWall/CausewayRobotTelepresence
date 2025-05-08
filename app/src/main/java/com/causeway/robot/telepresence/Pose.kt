package com.causeway.robot.telepresence

class Pose {
    var px: Float = 0f
    var py: Float = 0f
    var theta: Float = 0f
    val time: Long = 0
    var name: String? = null

    /**
     * SAFE = 0;      // normal area
     * NOT_SAFE = 1;  // dangerous area
     * OBSTACLE = 2;  // forbidden area
     * OUTSIDE = 3;   // out of the map
     */
    var status: Int = 0
    var distance: Float = 0f
}