// SPDX-License-Identifier: MIT
pragma solidity ^0.8.19;

import "@openzeppelin/contracts/token/ERC20/ERC20.sol";
import "@openzeppelin/contracts/access/Ownable.sol";
import "@openzeppelin/contracts/utils/Pausable.sol";
import "@openzeppelin/contracts/utils/ReentrancyGuard.sol";

/**
 * @title USDT - Tether USD
 * @dev ERC20 token representing Tether USD
 * @notice This is a simplified version of USDT for testing purposes
 */
contract USDT is ERC20, Ownable, Pausable, ReentrancyGuard {
    uint8 private constant DECIMALS = 6; // USDT has 6 decimals
    uint256 private constant INITIAL_SUPPLY = 1000000000 * 10**DECIMALS; // 1B USDT initial supply
    
    // Blacklist functionality
    mapping(address => bool) private _blacklisted;
    
    // Events
    event Mint(address indexed to, uint256 amount);
    event Burn(address indexed from, uint256 amount);
    event BlacklistUpdated(address indexed account, bool isBlacklisted);
    
    constructor() ERC20("Tether USD", "USDT") Ownable(msg.sender) {
        _mint(msg.sender, INITIAL_SUPPLY);
    }
    
    /**
     * @dev Returns the number of decimals used to get its user representation
     */
    function decimals() public pure override returns (uint8) {
        return DECIMALS;
    }
    
    /**
     * @dev Mint new tokens (only owner)
     * @param to Address to mint tokens to
     * @param amount Amount of tokens to mint
     */
    function mint(address to, uint256 amount) external onlyOwner whenNotPaused nonReentrant {
        require(to != address(0), "USDT: mint to zero address");
        require(amount > 0, "USDT: mint amount must be greater than 0");
        require(!_blacklisted[to], "USDT: cannot mint to blacklisted address");
        
        _mint(to, amount);
        emit Mint(to, amount);
    }
    
    /**
     * @dev Burn tokens from caller's balance
     * @param amount Amount of tokens to burn
     */
    function burn(uint256 amount) external nonReentrant {
        require(amount > 0, "USDT: burn amount must be greater than 0");
        require(balanceOf(msg.sender) >= amount, "USDT: burn amount exceeds balance");
        
        _burn(msg.sender, amount);
        emit Burn(msg.sender, amount);
    }
    
    /**
     * @dev Burn tokens from specified address (only owner)
     * @param from Address to burn tokens from
     * @param amount Amount of tokens to burn
     */
    function burnFrom(address from, uint256 amount) external onlyOwner nonReentrant {
        require(from != address(0), "USDT: burn from zero address");
        require(amount > 0, "USDT: burn amount must be greater than 0");
        require(balanceOf(from) >= amount, "USDT: burn amount exceeds balance");
        
        _burn(from, amount);
        emit Burn(from, amount);
    }
    
    /**
     * @dev Add address to blacklist (only owner)
     * @param account Address to blacklist
     */
    function addToBlacklist(address account) external onlyOwner {
        require(account != address(0), "USDT: blacklist zero address");
        require(!_blacklisted[account], "USDT: address already blacklisted");
        
        _blacklisted[account] = true;
        emit BlacklistUpdated(account, true);
    }
    
    /**
     * @dev Remove address from blacklist (only owner)
     * @param account Address to remove from blacklist
     */
    function removeFromBlacklist(address account) external onlyOwner {
        require(account != address(0), "USDT: blacklist zero address");
        require(_blacklisted[account], "USDT: address not blacklisted");
        
        _blacklisted[account] = false;
        emit BlacklistUpdated(account, false);
    }
    
    /**
     * @dev Check if address is blacklisted
     * @param account Address to check
     * @return True if address is blacklisted
     */
    function isBlacklisted(address account) external view returns (bool) {
        return _blacklisted[account];
    }
    
    /**
     * @dev Pause token transfers (only owner)
     */
    function pause() external onlyOwner {
        _pause();
    }
    
    /**
     * @dev Unpause token transfers (only owner)
     */
    function unpause() external onlyOwner {
        _unpause();
    }
    
    /**
     * @dev Hook that is called before any transfer of tokens
     */
    function _update(
        address from,
        address to,
        uint256 amount
    ) internal override whenNotPaused {
        require(!_blacklisted[from], "USDT: transfer from blacklisted address");
        require(!_blacklisted[to], "USDT: transfer to blacklisted address");
        
        super._update(from, to, amount);
    }
    
    /**
     * @dev Get the total supply of tokens
     */
    function getTotalSupply() external view returns (uint256) {
        return totalSupply();
    }
    
    /**
     * @dev Get the balance of an account
     */
    function getBalance(address account) external view returns (uint256) {
        return balanceOf(account);
    }
    
    /**
     * @dev Transfer tokens with additional checks
     * @param to Recipient address
     * @param amount Amount to transfer
     * @return True if transfer successful
     */
    function transfer(address to, uint256 amount) public override whenNotPaused nonReentrant returns (bool) {
        require(!_blacklisted[msg.sender], "USDT: transfer from blacklisted address");
        require(!_blacklisted[to], "USDT: transfer to blacklisted address");
        
        return super.transfer(to, amount);
    }
    
    /**
     * @dev Transfer tokens from one address to another with additional checks
     * @param from Sender address
     * @param to Recipient address
     * @param amount Amount to transfer
     * @return True if transfer successful
     */
    function transferFrom(
        address from,
        address to,
        uint256 amount
    ) public override whenNotPaused nonReentrant returns (bool) {
        require(!_blacklisted[from], "USDT: transfer from blacklisted address");
        require(!_blacklisted[to], "USDT: transfer to blacklisted address");
        
        return super.transferFrom(from, to, amount);
    }
}
