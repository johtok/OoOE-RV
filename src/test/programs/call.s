.text
    li a0, 20
    jal ra, fun
    j end

fun:
    addi a0, a0, 32
    ret

end:
    li a7, 10
    ecall