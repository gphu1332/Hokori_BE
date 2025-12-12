package com.hokori.web.dto.revenue;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Request DTO để teacher cập nhật thông tin tài khoản ngân hàng
 * Chỉ được phép khi teacher đã được APPROVED
 */
@Data
public class TeacherBankAccountUpdateReq {
    
    @NotBlank(message = "Số tài khoản không được để trống")
    @Size(max = 100, message = "Số tài khoản tối đa 100 ký tự")
    private String bankAccountNumber;
    
    @NotBlank(message = "Tên chủ tài khoản không được để trống")
    @Size(max = 150, message = "Tên chủ tài khoản tối đa 150 ký tự")
    private String bankAccountName;
    
    @NotBlank(message = "Tên ngân hàng không được để trống")
    @Size(max = 150, message = "Tên ngân hàng tối đa 150 ký tự")
    private String bankName;
    
    @Size(max = 150, message = "Tên chi nhánh tối đa 150 ký tự")
    private String bankBranchName; // Optional
}

